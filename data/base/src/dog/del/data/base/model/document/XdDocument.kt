package dog.del.data.base.model.document

import dog.del.commons.Date
import dog.del.commons.date
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.utils.xdRequiredDateProp
import dog.del.data.model.Document
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.FilteringContext.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.simple.*

class XdDocument(entity: Entity): XdEntity(entity), Document<XdDocumentType, XdUser> {
    companion object : XdNaturalEntityType<XdDocument>() {
        fun findOrNew(slug: String, template: (XdDocument.() -> Unit)? = null) = findOrNew(filter { it ->
            it.slug.eq(slug)
        }) {
            this.slug = slug
            template?.invoke(this)
        }

        fun find(slug: String) = filter { it ->
            it.slug.eq(slug)
        }.firstOrNull()

        override fun new(init: XdDocument.() -> Unit): XdDocument {
            return super.new {
                created = date()
                init(this)
            }
        }
    }

    override var slug by xdRequiredStringProp(unique = true, trimmed = true) {
        length(min = 3)
        regex(Regex("^[\\w-]*\$"))
    }

    override var type by xdLink1(XdDocumentType)

    // TODO: Require at least one of these to have a value depending on the type
    override var stringContent by xdBlobStringProp()
    override var blobContent by xdBlobProp()

    override var version by xdIntProp()
    override var owner by xdLink1(XdUser)

    // TODO: add support for expirable documents
    override var created by xdRequiredDateProp(
        default = { _, _ -> Date.getInstance() }
    )

    fun userCanEdit(user: XdUser) = owner == user || user.role.isAdmin
}