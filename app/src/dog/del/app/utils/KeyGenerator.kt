package dog.del.app.utils

import dog.del.commons.keygen.KeyGenerator
import dog.del.data.base.DB
import dog.del.data.base.Database
import dog.del.data.base.model.config.Config
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.xml.crypto.Data


fun KeyGenerator.createKey(db: TransientEntityStore, isUrl: Boolean): String {
    val config = runBlocking(Dispatchers.DB) { Config.getConfig(db) }
    return createKey(if (isUrl) config.urlKeyLength else config.pasteKeyLength)
}