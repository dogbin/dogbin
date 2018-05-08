import sys
from waitress import serve
from dogbin import app

this = sys.modules["dogbin"]

if __name__ == '__main__':
    serve(app, host=this.config['host'], port=this.config['port'])