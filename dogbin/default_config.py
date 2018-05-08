HOST = 'localhost'
PORT = 7777
DEBUG = True
APPNAME = 'dogbin'
MAX_DOCUMENT_LENGTH = 400000
KEY_GENERATOR = {
    'type': 'phonetic',
    'length': 10
}
URL_KEY_GENERATOR = {
    'type': 'random',
    'length': 7
}
STORAGE = {
    'type': 'file',
    'path': './data'
}
DOCUMENTS = {
    'about': './about.md',
    'changelog': './changelog.md'
}
MONGODB_SETTINGS = {
    'db':'dogbin_dev'
}
LOGGER_NAME = 'dogbin'
LOGGER_CONFIG = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'standard': {
            'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s - [in %(pathname)s:%(lineno)d]'
        },
        'short': {
            'format': '%(levelname)s - %(message)s - [in %(pathname)s:%(lineno)d]'
        }
    },
    'handlers': {
        'default': {
            'class': 'logging.handlers.TimedRotatingFileHandler',
            'level': 'WARN',
            'formatter': 'standard',
            'filename': 'logs/dogbin.log',
            'when': 'd',
            'interval': 3,
            'backupCount': 3
        },
        'console': {
            'class': 'logging.StreamHandler',
            'level': 'DEBUG',
            'formatter': 'short'
        },
    },
    'loggers': {
        'dogbin': {
            'handlers': ['default'],
            'level': 'DEBUG',
            'propagate': True
        },
        'werkzeug': {
            'propagate': True
        },
    },
    'root': {
        'level': 'DEBUG',
        'handlers': ['console']
    }
}
