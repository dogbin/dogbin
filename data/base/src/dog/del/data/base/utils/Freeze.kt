package dog.del.data.base.utils

import dog.del.data.base.Database
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.dnq.XdEntity

suspend fun XdEntity.freeze(store: TransientEntityStore): Map<String, Any?> = withContext(Database.dispatcher) {
    store.transactional(readonly = true) {
        entity.propertyNames.associateWith { entity.getProperty(it) }
    }
}

suspend fun XdEntity.updateFrom(store: TransientEntityStore, map: Map<String, Any?>) {
    withContext(Database.dispatcher) {
        store.transactional {
            for (prop in map) {
                entity.setProperty(prop.key, prop.value as Comparable<Any?>)
            }
        }
    }
}