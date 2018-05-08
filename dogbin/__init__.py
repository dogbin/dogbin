import json
import logging.config
from json import JSONEncoder
from os import path

import validators
from flask import (Flask, Response, jsonify, redirect, render_template,
                   request, send_from_directory, url_for)
from flask_mongoengine import MongoEngine

from dogbin import default_config
from dogbin.lib.model.document import Document

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
            document = Document(name, False, data, 0)
        ret = store.set(document, True)
        if(ret == False):
            app.logger.warn('couldn\'t load static document %s', name)
        else:
            app.logger.debug('loaded static document')


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
        document.increaseViewCount()
        if document.isUrl:
            app.logger.info('redirecting to %s', document.content)
            return redirect(document.content, 302)
        else:
            appname = app.config['APPNAME']
            lines = len(document.content.split('\n'))
            return render_template('index.html', document=document, lines=lines, title=f'{appname} - {document.slug}', lang=lang)
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


def handleDocument(content):
    keyLength = app.config['KEY_GENERATOR'].get('keyLength', 10)
    slug = keyGenerator.createKey(keyLength)
    while not store.slugAvailable(slug):
        slug = keyGenerator.createKey(keyLength)
    res = store.set(Document(slug, False, content))
    if(res == False):
        app.logger.info('error adding document')
        return jsonify({'message': 'Error adding document.'}), 500
    else:
        app.logger.info('added document %s', slug)
        return jsonify({'key': slug})


def handleUrl(content):
    keyLength = app.config['URL_KEY_GENERATOR'].get('keyLength', 7)
    slug = urlKeyGenerator.createKey(keyLength)
    while not store.slugAvailable(slug):
        slug = urlKeyGenerator.createKey(keyLength)
    res = store.set(Document(slug, True, content), True)
    if(res == False):
        app.logger.info('error adding url')
        return jsonify({'message': 'Error adding url.'}), 500
    else:
        app.logger.info('added url %s', slug)
        return jsonify({'key': slug})


@app.route('/documents', methods=['POST'])
def postDocument():
    ct = request.content_type
    content: str = None
    if(ct and ct.split(';')[0] == 'multipart/form-data'):
        content = request.forms.get('data').decode('utf-8').strip()
    else:
        content = request.data.decode('utf-8').strip()

    maxLength = app.config.get('MAX_DOCUMENT_LENGTH')
    if(maxLength and len(content) > maxLength):
        app.logger.warn('content >maxLength')
        return jsonify({'message': 'Content exceeds maximum length.'}), 400
    if(validators.url(content)):
        return handleUrl(content)
    else:
        return handleDocument(content)


@app.route('/')
def index():
    initialValue = ''
    duplicateFrom = request.args.get('duplicate')
    if(duplicateFrom):
        ret = store.get(duplicateFrom)
        if ret:
            initialValue = ret
    return render_template('index.html', title=app.config['APPNAME'], initialValue=initialValue)


def custom404(message: str):
    return jsonify({'message': message}), 404
