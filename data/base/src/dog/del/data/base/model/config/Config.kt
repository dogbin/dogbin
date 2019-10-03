package dog.del.data.base.model.config

import dog.del.commons.add
import dog.del.commons.date
import dog.del.data.base.utils.freeze
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.singleton.XdSingletonEntityType
import kotlinx.dnq.xdRequiredStringProp

class Config(entity: Entity) : XdEntity(entity) {
    companion object : XdSingletonEntityType<Config>() {
        override fun Config.initSingleton() {
            description = "The sexiest pastebin and url-shortener ever"
            keywords = "pastebin,code,log,url shortener"
        }

        fun getConfig(store: TransientEntityStore): Config = store.transactional {
            get()
        }
    }

    var description by xdRequiredStringProp()
    var keywords by xdRequiredStringProp()

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