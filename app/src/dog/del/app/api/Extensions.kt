package dog.del.app.api

import dog.del.data.base.Database
import dog.del.data.base.model.api.XdApiCredential
import io.ktor.application.ApplicationCall
import io.ktor.request.header
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.withContext

suspend fun ApplicationCall.apiCredentials(db: TransientEntityStore): XdApiCredential? =
    withContext(Database.dispatcher) {
        val key = request.header("X-Api-Key") ?: return@withContext null
        db.transactional(readonly = true) {
            XdApiCredential.find(key)
        }
    }