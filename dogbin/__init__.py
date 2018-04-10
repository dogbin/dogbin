import json
import sys

this = sys.modules["dogbin"]

this.config = json.load(open('config.json'))