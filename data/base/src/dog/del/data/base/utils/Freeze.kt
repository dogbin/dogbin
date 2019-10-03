package dog.del.data.base.utils

import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdEntity

fun XdEntity.freeze(store: TransientEntityStore) = store.transactional {
    entity.propertyNames.associateWith { entity.getProperty(it) }
}