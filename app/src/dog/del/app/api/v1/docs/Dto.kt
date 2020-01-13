package dog.del.app.api.v1.docs

import dog.del.commons.Date

data class DocumentListDto(
    val slug: String,
    val created: String,
    val link: String,
    val type: String
)