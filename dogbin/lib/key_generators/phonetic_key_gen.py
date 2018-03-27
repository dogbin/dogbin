from dogbin.lib.key_generators.key_generator import KeyGenerator
import logging
import random 

_vowels = "aeiou"
_consonants = "bcdfghjklmnpqrstvwxyz"

class PhoneticKeyGenerator(KeyGenerator):
    logger = logging.getLogger(__name__)

    def init(self, options:dict):
        # not needed for this generator
        return
    
    # Generate a phonetic key of alternating consonant & vowel
    def createKey(self, keyLength):
        text = ""
        start = random.randint(0, 1)
        for i in range(0, keyLength):
            if(i % 2 == start):
                text += random.choice(_consonants)
            else:
                text += random.choice(_vowels)
        return text

