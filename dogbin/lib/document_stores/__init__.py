from . import file


def getDocumentStore(app, options):
    type = options.get("type", None)
    if(type == "file"):
        return file.FileDocumentStore(app, options)
    else:
        app.logger.warning("%s is not a valid type", type)
        return file.FileDocumentStore(app, options)