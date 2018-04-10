from . import phonetic_key_gen, random_key_gen

def getKeyGenerator(app, options:dict):
    type = options.get("type", None)
    if(type == "random"):
        return random_key_gen.RandomKeyGenerator(app, options)
    elif(type == "phonetic"):
        return phonetic_key_gen.PhoneticKeyGenerator(app, options)
    else:
        app.logger.warning("%s is not a valid type", type)
        return random_key_gen.RandomKeyGenerator(app, options)

