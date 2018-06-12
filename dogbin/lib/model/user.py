import uuid
from flask_login import UserMixin
from dogbin import db
from flask_login import login_user
from dogbin.lib.model.document import Document

class User(UserMixin, db.Document):
    username = db.StringField(required=True, unique=True)
    is_active = db.BooleanField(required=True, default=True)
    is_anonymous = db.BooleanField(required=True, default=False)
    is_authenticated = db.BooleanField(required=True, default=False)
    roles = db.ListField(db.StringField())

    def can_edit(self, document:Document=None, slug:str=None) -> bool:
        from dogbin import store
        if not document:
            document = store.get(slug, True)
        return document.userCanEdit(self)

def anonymous() -> User:
    anon = User()
    anon.username = str(uuid.uuid1())
    anon.is_authenticated = False
    anon.is_anonymous = True
    anon.save()
    login_user(anon, remember=True)
    return anon
        