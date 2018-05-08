from dogbin.lib.document_stores.document_store import DocumentStore
from mongoengine import Document
from dogbin.lib.model.document import Document
from dogbin.lib.model.mongodocument import MongoDocument
from dogbin import db

class MongoDocumentStore(DocumentStore):

    def init(self, options):
        pass

    def set(self, document:Document, skipExpire:bool=False):
        MongoDocument.fromDocument(document).save()
    
    def get(self, slug:str, skipExpire:bool=False) -> MongoDocument:
        try:
            return MongoDocument.objects.get(slug=slug)
        except Exception as e:
            self.logger.warn(e)
            return False

    def slugAvailable(self, slug:str) -> bool:
        try:
            if MongoDocument.objects.get(slug=slug):
                return False
        except Exception as e:
            self.logger.debug(e)
        return True
        


