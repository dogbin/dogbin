class Document:
    slug:str
    isUrl:bool
    content:str
    viewCount:int

    def __init__(self, slug:str, isUrl:bool, content:str, viewCount:int=0):
        self.slug = slug
        self.isUrl = isUrl
        self.content = content
        self.viewCount = viewCount

    def increaseViewCount(self):
        pass