import logging
import sys
import validators
from json import JSONEncoder
from dogbin.lib import document_stores, key_generators
from bottle import Bottle, request, route, get, post, run, static_file, redirect, error, response, abort

app = Bottle()
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


@get('/<id>')
def idRoute(id):
    key = id.split('.')[0]
    ret = store.get(key)
    if(ret and validators.url(ret)):
        redirect(ret, 302)
        logger.info('redirected to %s', ret)
    else:
        return static_file('index.html', './static')

@get('/documents/<id>')
def getDocument(id):
    response.set_header('content-type', 'application/json')
    key = id.split('.')[0]
    skipExpire = key in this.config['documents']
    ret = store.get(key, skipExpire)
    if(ret == False):
        logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        logger.info('retrieved document %s', key)
        return JSONEncoder().encode({'data': ret, 'key': key})

@get('/raw/<id>')
def getDocumentRaw(id):
    response.set_header('content-type', 'text/plain')
    key = id.split('.')[0]
    skipExpire = key in this.config['documents']
    ret = store.get(key, skipExpire)
    if(ret == False):
        logger.warning('document not found %s', key)
        return custom404('Document not found.')
    else:
        logger.info('retrieved document %s', key)
        return ret
    

def handleDocument(content):
    key = ''
    while(store.get(key, True) != False):
        key = keyGenerator.createKey(this.config.get('keyLength', 10))
    res = store.set(key, content) 
    if(res == False):
        logger.info('error adding document')
        response.status = 500
        return JSONEncoder().encode({ 'message': 'Error adding document.' })
    else:
        logger.info('added document %s', key)
        return JSONEncoder().encode({ 'key': key })

def handleUrl(content):
    key = ''
    while(store.get(key, True) != False):
        key = urlKeyGenerator.createKey(this.config.get('urlKeyLength', 7))
    res = store.set(key, content, True) 
    if(res == False):
        logger.info('error adding url')
        response.status = 500
        return JSONEncoder().encode({ 'message': 'Error adding url.' })
    else:
        logger.info('added url %s', key)
        return JSONEncoder().encode({ 'key': key })

@post('/documents')
def postDocument():
    response.set_header('content-type', 'application/json')
    ct = request.content_type
    content:str = None
    if(ct and ct.split(';')[0] == 'multipart/form-data'):
        content = request.forms.get('data').decode("utf-8").strip()
    else:
        content = request.body.read().decode("utf-8").strip()

    maxLength = this.config.get('maxLength')
    if(maxLength and len(content) > maxLength):
        logger.warn('content >maxLength')
        response.status = 400
        return JSONEncoder().encode({ 'message': 'Content exceeds maximum length.' })
    if(validators.url(content)):
        return handleUrl(content)
    else:
        return handleDocument(content)


@route('/static/<filename>')
def staticFiles(filename):
    return static_file(filename, './static')


@route('/')
def index():
    return static_file('index.html', './static')


def custom404(message: str):
    response.set_header('content-type', 'application/json')
    response.status = 404
    return JSONEncoder().encode({'message': message})


run(host=this.config['host'], port=this.config['port'])
