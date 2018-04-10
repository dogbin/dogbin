import logging
import sys
import validators
from json import JSONEncoder
from dogbin.lib import document_stores, key_generators
from flask import Flask, render_template, send_from_directory, Response, request, redirect, url_for, jsonify

app = Flask(__name__)
logger = logging.getLogger(__name__)
this = sys.modules['dogbin']

store = document_stores.getDocumentStore(this.config['storage'])

# TODO recompress static assets

# Send the static documents into the store, skipping expirations
for name in this.config['documents']:
    path = this.config['documents'][name]
    with open(path) as file:
        data = file.read()
        logger.info('loading static document: %s - %s', name, path)
        ret = store.set(name, data, True)
        if(ret == False):
            logger.warn('couldn\'t load static document %s', name)
        else:
            logger.debug('loaded static document')

keyGenerator = key_generators.getKeyGenerator(this.config['keyGenerator'])

urlKeyGenerator = key_generators.getKeyGenerator(
    this.config['urlKeyGenerator'])


@app.route('/<id>')
def idRoute(id):
    key = id.split('.')[0]
    ret = store.get(key)
    if(ret and validators.url(ret)):
        redirect(ret, 302)
        logger.info('redirected to %s', ret)
    else:
        return render_template('index.html')

@app.route('/documents/<id>')
def getDocument(id):
    key = id.split('.')[0]
    skipExpire = key in this.config['documents']
    ret = store.get(key, skipExpire)
    if(ret == False):
        logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        logger.info('retrieved document %s', key)
        return jsonify({'data': ret, 'key': key})

@app.route('/raw/<id>')
def getDocumentRaw(id):
    key = id.split('.')[0]
    skipExpire = key in this.config['documents']
    ret = store.get(key, skipExpire)
    if(ret == False):
        logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        logger.info('retrieved document %s', key)
        return Response(ret, mimetype='text/plain')
    

def handleDocument(content):
    key = ''
    while(store.get(key, True) != False):
        key = keyGenerator.createKey(this.config.get('keyLength', 10))
    res = store.set(key, content) 
    if(res == False):
        logger.info('error adding document')
        return jsonify({ 'message': 'Error adding document.' }), 500
    else:
        logger.info('added document %s', key)
        return jsonify({ 'key': key })

def handleUrl(content):
    key = ''
    while(store.get(key, True) != False):
        key = urlKeyGenerator.createKey(this.config.get('urlKeyLength', 7))
    res = store.set(key, content, True) 
    if(res == False):
        logger.info('error adding url')
        return jsonify({ 'message': 'Error adding url.' }), 500
    else:
        logger.info('added url %s', key)
        return jsonify({ 'key': key })

@app.route('/documents', methods = ['POST'])
def postDocument():
    ct = request.content_type
    content:str = None
    if(ct and ct.split(';')[0] == 'multipart/form-data'):
        content = request.forms.get('data').decode('utf-8').strip()
    else:
        content = request.data.decode('utf-8').strip()

    maxLength = this.config.get('maxLength')
    if(maxLength and len(content) > maxLength):
        logger.warn('content >maxLength')
        return jsonify({ 'message': 'Content exceeds maximum length.' }), 400
    if(validators.url(content)):
        return handleUrl(content)
    else:
        return handleDocument(content)

@app.route('/')
def index():
    return render_template('index.html')


def custom404(message: str):
    return jsonify({'message': message}), 404 

app.run(host=this.config['host'], port=this.config['port'])