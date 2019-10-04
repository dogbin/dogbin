package dog.del.app.frontend

import dog.del.app.dto.FrontendDocumentDto
import dog.del.app.utils.slug
import dog.del.commons.Date
import dog.del.commons.year
import dog.del.data.base.Database
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.model.DocumentType
import io.ktor.application.call
import io.ktor.pebble.respondTemplate
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import java.util.*

fun Route.index() = route("/") {
    val store by inject<TransientEntityStore>()
    get {
        call.respondTemplate("index", mapOf(
            "title" to "dogbin"
        ))
    }

    get("/{slug}") {
        var documentDto: FrontendDocumentDto? = null
        store.transactional {
            val doc = XdDocument.find(call.slug)
            if (doc == null) {
                runBlocking {
                    call.respondRedirect("/")
                }
            } else {
                if (doc.type == XdDocumentType.URL) {
                    runBlocking {
                        call.respondRedirect(doc.stringContent!!, true)
                    }
                } else {
                    documentDto = FrontendDocumentDto.fromDocument(doc)
                }
            }
        }
        if (documentDto != null) {
            call.respondTemplate(
                "index", mapOf(
                    "title" to documentDto!!.title,
                    "description" to documentDto!!.description,
                    "document" to documentDto!!
                )
            )
        }
    }
}