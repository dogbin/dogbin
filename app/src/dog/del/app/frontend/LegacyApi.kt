package dog.del.app.frontend

import dog.del.app.utils.slug
import dog.del.commons.year
import dog.del.data.base.Database
import dog.del.data.base.model.document.XdDocument
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.pebble.respondTemplate
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

fun Route.legacyApi() = route("/") {
    val database by inject<TransientEntityStore>()
    get("raw/{slug}") {
        database.transactional(readonly = true) {
            val doc = XdDocument.find(call.slug)
            if (doc == null) {
                runBlocking {
                    call.respond(HttpStatusCode.NotFound, "No Document found")
                }
            } else {
                runBlocking {
                    call.respondText(doc.stringContent!!)
                }
            }
        }
    }
}