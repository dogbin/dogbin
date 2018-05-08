from dogbin.lib.document_stores.document_store import DocumentStore
from dogbin.lib.model.document import Document
import hashlib
import os, os.path

class FileDocumentStore(DocumentStore):

    def init(self, options):
        self.basePath = options.get("path", "./data")
        self.expire = options.get("expire", False)
    
    def getFilename(self, str):
        md5 = hashlib.md5()
        md5.update(str.encode())
        return self.basePath + "/" + md5.hexdigest()

    def set(self, document:Document, skipExpire:bool=False):
        if(not os.path.exists(self.basePath)):
            os.mkdir(self.basePath, 0o700)
        filename = self.getFilename(document.slug)
        file = open(filename, 'w')
        file.write(document.content)
        file.close()
        if(self.expire and not skipExpire):
            self.logger.warning("file store cannot set expirations on keys")
        return True
    
    def get(self, slug:str, skipExpire:bool=False) -> Document:
        try:
            filename = self.getFilename(slug)
            with open(filename) as file:
                data = file.read()
            if(self.expire and not skipExpire):
                self.logger.warning("file store cannot set expirations on keys")
            return Document(slug, None, data, 0)
        except FileNotFoundError as e:
            self.logger.warning("No file found in store for key %s", slug)
            self.logger.debug(e)
            return False

    def slugAvailable(self, slug:str) -> bool:
        filename = self.getFilename(slug)
        return not os.path.exists(filename)


