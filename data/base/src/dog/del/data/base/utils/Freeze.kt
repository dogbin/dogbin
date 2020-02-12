package dog.del.data.base.utils

import dog.del.data.base.DB
import dog.del.data.base.Database
import dog.del.data.base.suspended
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.dnq.XdEntity

suspend fun XdEntity.freeze(store: TransientEntityStore): Map<String, Any?> = withContext(Dispatchers.DB) {
    store.transactional(readonly = true) {
        entity.propertyNames.associateWith { entity.getProperty(it) }
    }
}

suspend fun XdEntity.updateFrom(store: TransientEntityStore, map: Map<String, Any?>) {
    store.suspended {
        for (prop in map) {
            entity.setProperty(prop.key, prop.value as Comparable<Any?>)
        }
    }
}