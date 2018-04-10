from dogbin.lib.key_generators.key_generator import KeyGenerator
import random 

class RandomKeyGenerator(KeyGenerator):

    def init(self, options:dict):
        self.keyspace = options.get("keyspace", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
    
    def createKey(self, keyLength):
        return "".join(random.choices(self.keyspace, k=keyLength)).lower()

