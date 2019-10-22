package dog.del.app.frontend

import dog.del.app.dto.CreateDocumentDto
import dog.del.app.dto.CreateDocumentResponseDto
import dog.del.app.session.user
import dog.del.app.utils.createKey
import dog.del.app.utils.slug
import dog.del.commons.isUrl
import dog.del.commons.keygen.KeyGenerator
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.getOrFail
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

fun Route.legacyApi() = route("/") {
    val db by inject<TransientEntityStore>()
    val slugGen by inject<KeyGenerator>()

    get("raw/{slug}") {
        db.transactional(readonly = true) {
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

    post("documents") {
        when (call.request.contentType().withoutParameters()) {
            ContentType.Application.Json -> {
                val dto = call.receive<CreateDocumentDto>()
                call.createDocument(dto, db, slugGen)
            }
            ContentType.MultiPart.FormData -> {
                val params = call.receiveParameters()
                val dto = CreateDocumentDto(
                    content = params.getOrFail("data"),
                    slug = params["slug"]
                )
                call.createDocument(dto, db, slugGen)
            }
            else -> {
                println(call.request.contentType())
                val dto = CreateDocumentDto(
                    content = call.receiveText()
                )
                call.createDocument(dto, db, slugGen)
            }
        }
    }
}

private suspend fun ApplicationCall.createDocument(
    dto: CreateDocumentDto,
    db: TransientEntityStore,
    slugGen: KeyGenerator
) {
    // TODO: uuh yea this ain't too beautiful
    val slugError = if (dto.slug != null) XdDocument.verifySlug(dto.slug) else null
    val result = if (dto.content.isBlank()) {
        HttpStatusCode.BadRequest to CreateDocumentResponseDto(
            message = "Paste content cannot be empty"
        )
    } else if (slugError != null) {
        HttpStatusCode.BadRequest to CreateDocumentResponseDto(
            message = slugError
        )
    } else db.transactional {
        val isFrontend = parameters["frontend"]?.toBoolean() ?: false
        val usr = user(db, !isFrontend)
        if (dto.slug.isNullOrBlank()) {
            val isUrl = dto.content.isUrl()
            val doc = XdDocument.new {
                slug = slugGen.createKey(db, isUrl)
                owner = usr
                stringContent = dto.content
                type = if (isUrl) XdDocumentType.URL else XdDocumentType.PASTE
            }
            HttpStatusCode.OK to CreateDocumentResponseDto(
                isUrl = isUrl,
                key = doc.slug
            )
        } else {
            // TODO: Properly validate slug and handle failures
            val doc = XdDocument.find(dto.slug)
            if (doc != null) {
                if (doc.userCanEdit(usr)) {
                    doc.stringContent = dto.content
                    HttpStatusCode.OK to CreateDocumentResponseDto(
                        isUrl = doc.type == XdDocumentType.URL,
                        key = doc.slug
                    )
                } else {
                    HttpStatusCode.Conflict to CreateDocumentResponseDto(
                        message = "This URL is already in use, please choose a different one"
                    )
                }
            } else {
                val isUrl = dto.content.isUrl()
                val doc = XdDocument.new {
                    slug = dto.slug
                    owner = usr
                    stringContent = dto.content
                    type = if (isUrl) XdDocumentType.URL else XdDocumentType.PASTE
                }

                HttpStatusCode.OK to CreateDocumentResponseDto(
                    isUrl = isUrl,
                    key = doc.slug
                )
            }
        }
    }
    respond(result.first, result.second)
}