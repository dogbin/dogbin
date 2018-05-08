from waitress import serve
from dogbin import app

if __name__ == '__main__':
    serve(app, host=app.config['HOST'], port=app.config['PORT'])