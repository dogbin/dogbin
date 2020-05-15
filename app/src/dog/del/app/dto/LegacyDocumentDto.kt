package dog.del.app.dto

import com.google.gson.annotations.SerializedName

data class LegacyDocumentDto(
    @SerializedName("key")
    val slug: String,
    @SerializedName("data")
    val content: String,
    val isEditable: Boolean,
    val type: DocumentTypeDto,
    val version: Int
)