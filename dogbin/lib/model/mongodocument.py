from dogbin import db
from dogbin.lib.model.document import Document

class MongoDocument(db.Document, Document):
    slug = db.StringField(require=True, db_field='_id')
    isUrl = db.BooleanField(required=True)
    content = db.StringField(required=True)
    viewCount = db.LongField(required=True)
    version = db.IntField(required=True, default=0)

    def fromDocument(self):
        doc = MongoDocument()
        doc.slug = self.slug
        doc.isUrl = self.isUrl
        doc.content = self.content
        doc.viewCount = self.viewCount
        doc.version = self.version
        return doc

    def increaseViewCount(self):
        self.viewCount += 1
        self.save()

    def increaseVersion(self):
        self.version += 1
        self.save()