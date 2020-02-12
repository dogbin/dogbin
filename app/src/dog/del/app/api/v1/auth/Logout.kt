package dog.del.app.api.v1.auth

import dog.del.data.base.Database
import dog.del.data.base.model.api.XdApiCredential
import dog.del.data.base.suspended
import dog.del.data.base.transactionalAsync
import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.util.getOrFail
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.logout(store: TransientEntityStore) = post("logout") {
    val key = call.parameters.getOrFail("key")
    store.suspended {
        XdApiCredential.find(key)?.delete()
    }
}