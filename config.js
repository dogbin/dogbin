{

  "host": "0.0.0.0",
  "port": 7777,

  "keyLength": 10,

  "urlKeyLength": 7,

  "maxLength": 400000,

  "staticMaxAge": 86400,

  "recompressStaticAssets": true,

  "logging": [
    {
      "level": "verbose",
      "type": "Console",
      "colorize": true
    }
  ],

  "keyGenerator": {
    "type": "phonetic"
  },

  "urlKeyGenerator": {
    "type": "random"
  },

  "rateLimits": {
    "categories": {
      "normal": {
        "totalRequests": 500,
        "every": 60000
      }
    }
  },

  "storage": {
    "path": "./data",
    "type": "file"
  },

  "documents": {
    "about": "./about.md"
  }

}
