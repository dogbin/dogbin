from . import file
from . import mongo


def getDocumentStore(app, options):
    type = options.get("type", None)
    if(type == "file"):
        return file.FileDocumentStore(app, options)
    if(type == "mongo"):
        return mongo.MongoDocumentStore(app, options)
    else:
        app.logger.warning("%s is not a valid type", type)
        return file.FileDocumentStore(app, options)