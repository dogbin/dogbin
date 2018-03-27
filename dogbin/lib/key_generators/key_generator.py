import logging
class KeyGenerator:
    logger = logging.getLogger(__name__)
    def __init__(self, options:dict):
        self.logger.debug("Initializing %s", self.__class__.__name__)
        self.init(options)

    def init(self, options:dict):
        self.logger.error("%s doesn't implement init()", self.__class__.__name__)
        raise NotImplementedError

    def createKey(self, keyLength:int):
        self.logger.error("%s doesn't implement set()", self.__class__.__name__)
        raise NotImplementedError