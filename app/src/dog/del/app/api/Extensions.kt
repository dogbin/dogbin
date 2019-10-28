package dog.del.app.api

import dog.del.data.base.model.api.XdApiCredential
import io.ktor.application.ApplicationCall
import io.ktor.request.header
import jetbrains.exodus.database.TransientEntityStore

fun ApplicationCall.apiCredentials(db: TransientEntityStore): XdApiCredential? {
    val key = request.header("X-Api-Key") ?: return null
    return db.transactional(readonly = true) {
        XdApiCredential.find(key)
    }
}