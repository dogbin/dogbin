package dog.del.app.api.v1.docs

import dog.del.app.api.apiCredentials
import dog.del.commons.formatISO
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.suspended
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.url
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.sortedBy
import kotlinx.dnq.query.toList

fun Route.list(store: TransientEntityStore) = get {
    val creds = call.apiCredentials(store)
    if (creds == null) {
        call.respond(HttpStatusCode.Unauthorized, "Missing/invalid api key")
        return@get
    }
    val canList = store.suspended(readonly = true) { creds.canListDocuments }
    if (!canList) {
        call.respond(HttpStatusCode.Unauthorized, "Missing 'list' permission")
        return@get
    }

    val docList = store.suspended(readonly = true) {
        XdDocument.filter { it.owner eq creds.user }.sortedBy(XdDocument::created, asc = false).toList().map {
            DocumentListDto(
                it.slug,
                it.created.formatISO(),
                url {
                    val origin = call.request.origin
                    protocol = URLProtocol.createOrDefault(origin.scheme)
                    port = origin.port
                    host = origin.host
                    path(it.slug)
                },
                it.type.name.toLowerCase()
            )
        }
    }
    call.respond(docList)
}
