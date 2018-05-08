from dogbin.lib.model.document import Document

class DocumentStore:
    def __init__(self, app, options):
        self.logger = app.logger
        self.logger.debug("Initializing %s", self.__class__.__name__)
        self.init(options)

    def init(self, options):
        self.logger.error("%s doesn't implement init()", self.__class__.__name__)
        raise NotImplementedError

    def set(self, document:Document, skipExpire:bool=False):
        self.logger.error("%s doesn't implement set()", self.__class__.__name__)
        raise NotImplementedError

    def get(self, slug:str, skipExpire:bool=False) -> Document:
        self.logger.error("%s doesn't implement get()", self.__class__.__name__)
        raise NotImplementedError

    def slugAvailable(self, slug:str) -> bool:
        self.logger.error("%s doesn't implement hasKey()", self.__class__.__name__)
        raise NotImplementedError