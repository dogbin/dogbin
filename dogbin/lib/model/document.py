class Document:
    slug:str
    isUrl:bool
    content:str
    viewCount:int
    version:int
    owner:object

    def __init__(self, slug:str, isUrl:bool, content:str, viewCount:int=0, version:int=0, owner:object=None):
        self.slug = slug
        self.isUrl = isUrl
        self.content = content
        self.viewCount = viewCount
        self.version = version
        self.owner = owner

    def increaseViewCount(self):
        self.viewCount += 1

    def update_content(self, content:str):
        self.content = content
        self.version += 1

    def userCanEdit(self, user):
        return False