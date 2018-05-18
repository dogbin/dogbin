class Document:
    slug:str
    isUrl:bool
    content:str
    viewCount:int
    version:int

    def __init__(self, slug:str, isUrl:bool, content:str, viewCount:int=0, version:int=0):
        self.slug = slug
        self.isUrl = isUrl
        self.content = content
        self.viewCount = viewCount
        self.version = version

    def increaseViewCount(self):
        self.viewCount += 1

    def increaseVersion(self):
        self.version += 1