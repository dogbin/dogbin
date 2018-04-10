class KeyGenerator:
    def __init__(self, app, options:dict):
        self. logger = app.logger
        self.logger.debug("Initializing %s", self.__class__.__name__)
        self.init(options)

    def init(self, options:dict):
        self.logger.error("%s doesn't implement init()", self.__class__.__name__)
        raise NotImplementedError

    def createKey(self, keyLength:int):
        self.logger.error("%s doesn't implement set()", self.__class__.__name__)
        raise NotImplementedError