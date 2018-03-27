import logging
from . import phonetic_key_gen, random_key_gen

logger = logging.getLogger(__name__)

def getKeyGenerator(options:dict):
    type = options.get("type", None)
    if(type == "random"):
        return random_key_gen.RandomKeyGenerator(options)
    elif(type == "phonetic"):
        return phonetic_key_gen.PhoneticKeyGenerator(options)
    else:
        logger.warning("%s is not a valid type", type)
        return random_key_gen.RandomKeyGenerator(options)

