package dog.del.data.base.model.api

import com.sun.org.apache.xml.internal.security.algorithms.implementations.SignatureDSA
import dog.del.commons.Date
import dog.del.commons.date
import dog.del.commons.sha256
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.utils.xdRequiredDateProp
import dog.del.data.model.ApiCredential
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.toList

class XdApiCredential(entity: Entity) : XdEntity(entity), ApiCredential<XdUser> {
    companion object : XdNaturalEntityType<XdApiCredential>() {
        fun new(key: String, user: XdUser): XdApiCredential? {
            return new {
                keyHash = key.sha256()
                this.user = user
            }
        }

        fun find(key: String): XdApiCredential? {
            val hash = key.sha256()
            return filter { it.keyHash eq hash }.firstOrNull()
        }

        fun findForUser(user: XdUser) = filter { it.user eq user }.toList()
    }

    override fun constructor() {
        super.constructor()
        // Initialize date
        created = date()
    }

    override var keyHash by xdRequiredStringProp(unique = true)
    override var name by xdRequiredStringProp(trimmed = true)

    override var user by xdLink1(XdUser)

    override var created by xdRequiredDateProp(
        default = { _, _ -> date() }
    )

    override var canCreateDocuments by xdBooleanProp()
    override var canUpdateDocuments by xdBooleanProp()
    override var canDeleteDocuments by xdBooleanProp()
    override var canListDocuments by xdBooleanProp()
}