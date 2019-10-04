package dog.del.data.base.model.session

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.FilteringContext.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.xdBlobProp
import kotlinx.dnq.xdRequiredStringProp

class XdSession(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<XdSession>() {
        fun find(id: String) = filter { it.id eq id }.firstOrNull()
        fun findOrNew(id: String, template: (XdSession.() -> Unit)? = null) = findOrNew(filter { it ->
            it.id.eq(id)
        }) {
            this.id = id
            template?.invoke(this)
        }
    }
    var id by xdRequiredStringProp(unique = true)
    var content by xdBlobProp()
}