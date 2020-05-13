package dog.del.app.api.v1.docs

import dog.del.app.api.ErrorDto
import dog.del.app.api.apiCredentials
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.suspended
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import jetbrains.exodus.database.TransientEntityStore

data class EditableDtoIn(
    val slug: String
)

data class EditableDtoOut(
    val slug: String,
    val editable: Boolean
)

fun Route.editable(store: TransientEntityStore) = get("/editable") {
    val data = call.receive<EditableDtoIn>()

    val creds = call.apiCredentials(store)
    if (creds == null) {
        call.respond(HttpStatusCode.Unauthorized, ErrorDto("Missing/invalid api key"))
        return@get
    }

    val docEditable = store.suspended(readonly = true) {
        val targetDocument = XdDocument.find(data.slug) ?: return@suspended null
        return@suspended EditableDtoOut(data.slug, creds.user == targetDocument.owner)
    }

    if (docEditable != null) {
        call.respond(docEditable)
    } else {
        call.respond(HttpStatusCode.NotFound, ErrorDto("Document not found"))
    }
}
