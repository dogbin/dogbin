import json
import logging, logging.config
import sys

this = sys.modules["dogbin"]


this.config = json.load(open('config.json'))
logging.config.dictConfig(this.config["logging"])