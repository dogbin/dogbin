package dog.del.data.base.model.config

import dog.del.commons.add
import dog.del.commons.date
import dog.del.data.base.utils.freeze
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.singleton.XdSingletonEntityType
import kotlinx.dnq.xdIntProp
import kotlinx.dnq.xdRequiredStringProp
import kotlinx.dnq.xdStringProp

class Config(entity: Entity) : XdEntity(entity) {
    companion object : XdSingletonEntityType<Config>() {
        override fun Config.initSingleton() {
            initDefaults()
        }

        fun getConfig(store: TransientEntityStore): Config = store.transactional {
            get().apply { initDefaults() }
        }
    }

    private fun initDefaults() {
        if ("description" !in entity.propertyNames)
            description = "The sexiest pastebin and url-shortener ever"
        if ("keywords" !in entity.propertyNames)
            keywords = "pastebin,code,log,url shortener"
        if ("pasteKeyLength" !in entity.propertyNames)
            pasteKeyLength = 10
        if ("urlKeyLength" !in entity.propertyNames)
            urlKeyLength = 7
    }

    var description by xdRequiredStringProp()
    var keywords by xdRequiredStringProp()
    var pasteKeyLength by xdIntProp()
    var urlKeyLength by xdIntProp()

    // Cache frozen config for 10 minutes
    // TODO: make this work on all entities
    private var updatedAt = date(0)
    private val nextUpdate get() = updatedAt.add(minutes = 10)
    private val needsUpdate get() = date() > nextUpdate
    private var freezeCache: Map<String, Any?>? = null
        set(value) {
            updatedAt = date()
            field = value
        }

    fun freezeCached(store: TransientEntityStore): Map<String, Any?> {
        if (needsUpdate) {
            freezeCache = freeze(store)
        }
        return freezeCache ?: emptyMap()
    }
}