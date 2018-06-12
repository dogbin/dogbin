import uuid
import bcrypt
from flask_login import UserMixin
from dogbin import db
from flask_login import login_user
from dogbin.lib.model.document import Document

class User(UserMixin, db.Document):
    username = db.StringField(required=True, unique=True)
    password = db.BinaryField(required=True)
    is_active = db.BooleanField(required=True, default=True)
    is_anonymous = db.BooleanField(required=True, default=False)
    is_system = db.BooleanField(required=True, default=False)
    is_authenticated = db.BooleanField(required=True, default=False)
    roles = db.ListField(db.StringField())

    def can_edit(self, document:Document=None, slug:str=None) -> bool:
        from dogbin import store
        if not document:
            document = store.get(slug, True)
        return document.userCanEdit(self)

    def set_password(self, password:str):
        self.password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

    def check_password(self, password:str) -> bool:
        return bcrypt.checkpw(password.encode('utf-8'), self.password)

    def get_display_name(self) -> str:
        if self.is_anonymous:
            return 'Anonymous'
        else:
            return self.username

def anonymous() -> User:
    anon = User()
    anon.username = str(uuid.uuid1())
    anon.is_anonymous = True
    anon.set_password(anon.username)
    anon.save()
    login_user(anon, remember=True)
    return anon

def system_user() -> User:
    dgb = User.objects(username='dogbin').get(0)
    if not dgb:
        dgb = User()
        dgb.username = 'dogbin'
        dgb.is_system = True
        dgb.set_password(dgb.username)
        dgb.save()
    return dgb