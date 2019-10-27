package dog.del.data.base.utils

import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdEntity

fun XdEntity.freeze(store: TransientEntityStore): Map<String, Any?> = store.transactional(readonly = true) {
    entity.propertyNames.associateWith { entity.getProperty(it) }
}

fun XdEntity.updateFrom(store: TransientEntityStore, map: Map<String, Any?>) {
    store.transactional {
        for (prop in map) {
            entity.setProperty(prop.key, prop.value as Comparable<Any?>)
        }
    }
}