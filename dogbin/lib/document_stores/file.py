from dogbin.lib.document_stores.document_store import DocumentStore
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

    def set(self, key:str, data, skipExpire:bool=False):
        if(not os.path.exists(self.basePath)):
            os.mkdir(self.basePath, 0o700)
        filename = self.getFilename(key)
        file = open(filename, 'w')
        file.write(data)
        file.close()
        if(self.expire and not skipExpire):
            self.logger.warning("file store cannot set expirations on keys")
        return True
    
    def get(self, key:str, skipExpire:bool=False):
        try:
            filename = self.getFilename(key)
            with open(filename) as file:
                data = file.read()
            if(self.expire and not skipExpire):
                self.logger.warning("file store cannot set expirations on keys")
            return data
        except FileNotFoundError as e:
            self.logger.warning("No file found in store for key %s", key)
            self.logger.debug(e)
            return False


