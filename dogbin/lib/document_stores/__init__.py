import logging
from . import file

logger = logging.getLogger(__name__)

def getDocumentStore(options):
    type = options.get("type", None)
    if(type == "file"):
        return file.FileDocumentStore(options)
    else:
        logger.warning("%s is not a valid type", type)
        return file.FileDocumentStore(options)