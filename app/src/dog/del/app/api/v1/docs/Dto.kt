package dog.del.app.api.v1.docs

data class DocumentListDto(
    val slug: String,
    val created: String,
    val link: String,
    val type: String,
    val content: String
)
