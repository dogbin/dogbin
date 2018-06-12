import json
import re
import logging.config
from json import JSONEncoder
from os import path

import validators
from flask import (Flask, Response, jsonify, redirect, render_template,
                   request, send_from_directory, url_for, session, abort)
from flask_mongoengine import MongoEngine
from flask_login import LoginManager, current_user, login_user

from dogbin import default_config
from dogbin.lib.model.document import Document
from datetime import datetime

app = Flask(__name__)
app.config.from_object(default_config)
if path.exists('config.py'):
    import config
    app.config.from_object(config)

# Initialise loggers
app.logger
logging.config.dictConfig(app.config['LOGGER_CONFIG'])

# Initialise MongoEngine
db = MongoEngine(app)

# Initialise flask-login
from dogbin.lib.model.user import User, anonymous, system_user
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.anonymous_user = anonymous

from dogbin.lib import document_stores, key_generators

with app.app_context():
    store = document_stores.getDocumentStore(app, app.config['STORAGE'])
    keyGenerator = key_generators.getKeyGenerator(
        app, app.config['KEY_GENERATOR'])
    urlKeyGenerator = key_generators.getKeyGenerator(
        app, app.config['URL_KEY_GENERATOR'])

# TODO recompress static assets

# Send the static documents into the store, skipping expirations
for name in app.config['DOCUMENTS']:
    path = app.config['DOCUMENTS'][name]
    with open(path) as file:
        data = file.read()
        app.logger.info('loading static document: %s - %s', name, path)
        document = store.get(name, True)
        if not document:
            document = Document(name, False, data, owner=system_user())
        else:
            app.logger.info('document already in store')
            if document.content != data:
                app.logger.info('content on disk is different than store content. updating...')
                document.content = data
                document.viewCount = 0
                document.version += 1
            if document.owner != system_user():
                document.owner = system_user()
        ret = store.set(document, True)
        if(ret == False):
            app.logger.warn('couldn\'t load static document %s', name)
        else:
            app.logger.debug('loaded static document')

# Get user for user id
@login_manager.user_loader
def load_user(user_id):
    try:
        return User.objects.get(id=user_id)
    except Exception:
        app.logger.info(f'No user found with user id \'{user_id}\'')
        return None

@app.route('/login', methods=['GET', 'POST'])
def do_login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        query = User.objects(username=username)
        if query:
            user:User = query.get(0)
            if not user.is_anonymous and user.check_password(password):
                login_user(user, remember=True)
                return redirect('/')
        return abort(401)
    else:
        return '''
            <form action="" method="post">
                <p><input type=text name=username required>
                <p><input type=password name=password required>
                <p><input type=submit value=Login>
            </form>
            '''

@app.route('/register', methods=['GET', 'POST'])
def do_register():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        if current_user.is_anonymous:
            current_user.username = username
            current_user.is_anonymous = False
            current_user.set_password(password)
            current_user.save()
            login_user(current_user, remember=True)
            app.logger.debug(current_user)
        return redirect('/')
    else:
        return '''
            <form action="" method="post">
                <p><input type=text name=username required>
                <p><input type=password name=password required>
                <p><input type=submit value=Register>
            </form>
            '''

def viewed(document: Document):
    if not 'viewed' in session:
        session['viewed'] = []
    key = f'{document.slug}@{document.version}'
    if not key in session['viewed']: 
        document.increaseViewCount()
        session['viewed'].append(key)
        session.modified = True
        
@app.route('/<id>')
def idRoute(id):
    parts = id.split('.')
    key = parts[0]
    lang = ''
    if len(parts) > 1:
        lang = parts[1]
        if lang == 'txt':
            lang = 'nohighlight'
    document = store.get(key)
    if document:
        viewed(document)
        if document.isUrl:
            app.logger.info('redirecting to %s', document.content)
            return redirect(document.content, 302)
        else:
            appname = app.config['APPNAME']
            lines = len(document.content.split('\n'))
            return render_template('index.html', document=document, lines=lines, title=f'{appname} - {document.slug}', lang=lang)
    else:
        return redirect('/', 302)

@app.route('/v/<slug>')
def viewRoute(slug):
    parts = slug.split('.')
    key = parts[0]
    lang = ''
    if len(parts) > 1:
        lang = parts[1]
        if lang == 'txt':
            lang = 'nohighlight'
    document = store.get(key)
    if document:
        viewed(document)
        if(document.isUrl):
            lang = 'nohighlight'
        appname = app.config['APPNAME']
        lines = len(document.content.split('\n'))
        return render_template('index.html', document=document, lines=lines, title=f'{appname} - {document.slug}', lang=lang)
    else:
        return redirect('/', 302)

@app.route('/e/<slug>')
def edit_document(slug):
    key = slug.split('.')[0]
    doc = store.get(key)
    if doc and doc.userCanEdit(current_user):
        initialValue = doc.content
        appname = app.config['APPNAME']
        return render_template('index.html', title=f'{appname} - editing {key}', initialValue=initialValue, edit_key=key)
    else:
        return redirect('/', 302)

@app.route('/d/<slug>')
def duplicate_document(slug):
    key = slug.split('.')[0]
    doc = store.get(key)
    if doc:
        return render_template('index.html', title=app.config['APPNAME'], initialValue=doc.content)
    else:
        return redirect('/', 302)

@app.route('/documents/<slug>')
def getDocument(slug):
    key = slug.split('.')[0]
    skipExpire = key in app.config['DOCUMENTS']
    document = store.get(key, skipExpire)
    if not document:
        app.logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        app.logger.info('retrieved document %s', key)
        return jsonify({'document': document, 'data': document.content, 'key': key})


@app.route('/raw/<slug>')
def getDocumentRaw(slug):
    key = slug.split('.')[0]
    skipExpire = key in app.config['DOCUMENTS']
    document = store.get(key, skipExpire)
    if not document:
        app.logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        app.logger.info('retrieved document %s', key)
        return Response(document.content, mimetype='text/plain')


def handleDocument(content, slug:str, update:bool = False):
    res = None
    if not slug and not update: 
        keyLength = app.config['KEY_GENERATOR'].get('keyLength', 10)
        slug = keyGenerator.createKey(keyLength)
        while not store.slugAvailable(slug):
            slug = keyGenerator.createKey(keyLength)
    if update:
        res = store.get(slug, True)
        res.update_content(content)
    else:
        res = store.set(Document(slug, False, content, owner=current_user._get_current_object()))
    if(not res):
        app.logger.info('error adding document')
        return jsonify({'message': 'Error adding document.'}), 500
    else:
        app.logger.info('added document %s', slug)
        return jsonify({'key': slug, 'isUrl': False})


def handleUrl(content, slug:str, update:bool = False):
    res = None
    if not slug and not update: 
        keyLength = app.config['URL_KEY_GENERATOR'].get('keyLength', 7)
        slug = urlKeyGenerator.createKey(keyLength)
        while not store.slugAvailable(slug):
            slug = urlKeyGenerator.createKey(keyLength)
    if update:
        res = store.get(slug, True)
        res.update_content(content)
    else:
        res = store.set(Document(slug, True, content, owner=current_user._get_current_object()), True)
    if(not res):
        app.logger.info('error adding url')
        return jsonify({'message': 'Error adding url.'}), 500
    else:
        app.logger.info('added url %s', slug)
        return jsonify({'key': slug, 'isUrl': True})


@app.route('/documents', methods=['POST'])
def postDocument():
    ct = request.content_type
    content: str = None
    slug: str = None
    update: bool = False
    if(ct and ct.split(';')[0] == 'multipart/form-data'):
        content = request.forms.get('data').decode('utf-8').strip()
        slug = request.forms.get('slug')
        if slug:
            slug = slug.decode('utf-8').strip()
    elif request.json:
        content = request.json.get('content').strip()
        slug = request.json.get('slug')
        if slug:
            slug = slug.strip()
    else:
        content = request.get_data().decode('utf-8').strip()

    maxLength = app.config.get('MAX_DOCUMENT_LENGTH')
    if(maxLength and len(content) > maxLength):
        app.logger.warn('content >maxLength')
        return jsonify({'message': 'Content exceeds maximum length.'}), 400

    if slug:
        if len(slug) < 3:
            return jsonify({'message': 'Custom URLs need to be atleast 3 characters long'}), 400
        if not re.match(r'^[\w-]*$', slug):
            return jsonify({'message': 'Custom URLs must be alphanumeric and cannot contain spaces'}), 400
        doc = store.get(slug)
        if doc:
            if not doc.userCanEdit(current_user):
                return jsonify({'message': 'This URL is already in use, please choose a different one'}), 409
            else:
                update = True

    if(validators.url(content)):
        return handleUrl(content, slug, update)
    else:
        return handleDocument(content, slug, update)


@app.route('/')
def index():
    return render_template('index.html', title=app.config['APPNAME'])

def custom404(message: str):
    return jsonify({'message': message}), 404

@app.context_processor
def inject_now():
    return {'now': datetime.utcnow()}
