import json
import logging
from json import JSONEncoder
from os import path

import validators
from flask import (Flask, Response, jsonify, redirect, render_template,
                   request, send_from_directory, url_for)

from dogbin import default_config
from dogbin.lib import document_stores, key_generators

app = Flask(__name__)
app.config.from_object(default_config)
if path.exists('config.py'):
    import config
    app.config.from_object(config)

# Initialise loggers
app.logger
logging.config.dictConfig(app.config['LOGGER_CONFIG'])


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
        ret = store.set(name, data, True)
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
    ret = store.get(key)
    if ret:
        if validators.url(ret):
            redirect(ret, 302)
            app.logger.info('redirected to %s', ret)
        else:
            appname = app.config['APPNAME']
            lines = len(str(ret).split('\n'))
            return render_template('index.html', content=ret, key=key, lines=lines, title=f'{appname} - {key}', lang=lang)
    else:
        return redirect('/', 302)


@app.route('/documents/<id>')
def getDocument(id):
    key = id.split('.')[0]
    skipExpire = key in app.config['DOCUMENTS']
    ret = store.get(key, skipExpire)
    if(ret == False):
        app.logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        app.logger.info('retrieved document %s', key)
        return jsonify({'data': ret, 'key': key})


@app.route('/raw/<id>')
def getDocumentRaw(id):
    key = id.split('.')[0]
    skipExpire = key in app.config['DOCUMENTS']
    ret = store.get(key, skipExpire)
    if(ret == False):
        app.logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        app.logger.info('retrieved document %s', key)
        return Response(ret, mimetype='text/plain')


def handleDocument(content):
    key = ''
    while(store.get(key, True) != False):
        key = keyGenerator.createKey(app.config['KEY_GENERATOR'].get('keyLength', 10))
    res = store.set(key, content)
    if(res == False):
        app.logger.info('error adding document')
        return jsonify({'message': 'Error adding document.'}), 500
    else:
        app.logger.info('added document %s', key)
        return jsonify({'key': key})


def handleUrl(content):
    key = ''
    while(store.get(key, True) != False):
        key = urlKeyGenerator.createKey(app.config['URL_KEY_GENERATOR'].get('keyLength', 7))
    res = store.set(key, content, True)
    if(res == False):
        app.logger.info('error adding url')
        return jsonify({'message': 'Error adding url.'}), 500
    else:
        app.logger.info('added url %s', key)
        return jsonify({'key': key})


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
