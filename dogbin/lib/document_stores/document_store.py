class DocumentStore:
    def __init__(self, app, options):
        self.logger = app.logger
        self.logger.debug("Initializing %s", self.__class__.__name__)
        self.init(options)

    def init(self, options):
        self.logger.error("%s doesn't implement init()", self.__class__.__name__)
        raise NotImplementedError

    def set(self, key:str, data, skipExpire:bool=False):
        self.logger.error("%s doesn't implement set()", self.__class__.__name__)
        raise NotImplementedError

    def get(self, key:str, skipExpire:bool=False):
        self.logger.error("%s doesn't implement get()", self.__class__.__name__)
        raise NotImplementedError