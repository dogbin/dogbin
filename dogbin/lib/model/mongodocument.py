from dogbin import db
from dogbin.lib.model.document import Document

class MongoDocument(db.Document, Document):
    slug = db.StringField(require=True, db_field='_id')
    isUrl = db.BooleanField(required=True)
    content = db.StringField(required=True)
    viewCount = db.LongField(required=True)
    version = db.IntField(required=True, default=0)
    owner = db.ReferenceField(document_type='User', reverse_delete_rule=2)

    def fromDocument(self):
        doc = MongoDocument()
        doc.slug = self.slug
        doc.isUrl = self.isUrl
        doc.content = self.content
        doc.viewCount = self.viewCount
        doc.version = self.version
        doc.owner = self.owner
        return doc

    def increaseViewCount(self):
        self.viewCount += 1
        self.save()

    def update_content(self, content:str):
        self.content = content
        self.version += 1
        self.save()

    def userCanEdit(self, user):
        return self.owner == user or 'admin' in user.roles