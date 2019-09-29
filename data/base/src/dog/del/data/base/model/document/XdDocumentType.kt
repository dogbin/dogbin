package dog.del.data.base.model.document

import dog.del.data.model.DocumentType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.enum.XdEnumEntityType

class XdDocumentType(entity: Entity) : XdEnumEntity(entity), DocumentType {
    companion object : XdEnumEntityType<XdDocumentType>() {
        val PASTE by enumField { }
        val URL by enumField { }

        // TODO: add support for images and general files
    }
}