package dog.del.app.dto

import dog.del.commons.lineCount
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.model.Document
import dog.del.data.model.DocumentType

data class DocumentDto(
    val slug: String,
    val type: DocumentTypeDto,
    val content: String?,
    val owner: UserDto
) {
    // Disable for rendered markdown content
    val showLines = true
    val lines = content?.lineCount ?: 0
    // Use frontmatter data for rendered markdown content
    val description = content?.take(100) ?: "The sexiest pastebin and url-shortener ever"
    val title = "dogbin - $slug"
    companion object {
        fun fromDocument(document: Document<XdDocumentType, *>) = DocumentDto(
            document.slug,
            DocumentTypeDto.fromXdDocumentType(document.type),
            document.stringContent,
            UserDto.fromUser(document.owner)
        )
    }
}

enum class DocumentTypeDto{
    URL, PASTE;

    companion object {
        fun fromXdDocumentType(documentType: XdDocumentType) = when(documentType) {
            XdDocumentType.PASTE -> PASTE
            XdDocumentType.URL -> URL
            else -> throw IllegalStateException()
        }
    }
}