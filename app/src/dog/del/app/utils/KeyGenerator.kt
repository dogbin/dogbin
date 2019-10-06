package dog.del.app.utils

import dog.del.commons.keygen.KeyGenerator
import dog.del.data.base.model.config.Config
import jetbrains.exodus.database.TransientEntityStore


fun KeyGenerator.createKey(db: TransientEntityStore, isUrl: Boolean): String {
    val config = Config.getConfig(db)
    return createKey(if (isUrl) config.urlKeyLength else config.pasteKeyLength)
}