package dog.del.app.frontend

import dog.del.app.api.apiCredentials
import dog.del.app.dto.CreateDocumentDto
import dog.del.app.dto.CreateDocumentResponseDto
import dog.del.app.highlighter.Highlighter
import dog.del.app.session.user
import dog.del.app.stats.StatisticsReporter
import dog.del.app.stats.StatisticsReporter.Event
import dog.del.app.utils.createKey
import dog.del.app.utils.get
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
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext
import org.koin.ktor.ext.inject

fun Route.legacyApi() = route("/") {
    val db by inject<TransientEntityStore>()
    val slugGen by inject<KeyGenerator>()
    val reporter by inject<StatisticsReporter>()

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
                val slug = doc.slug
                GlobalScope.launch {
                    reporter.reportImpression(slug, false, call.request)
                }
            }
        }
    }

    post("documents") {
        when (call.request.contentType().withoutParameters()) {
            ContentType.Application.Json -> {
                val dto = call.receive<CreateDocumentDto>()
                call.createDocument(dto, db, slugGen, reporter)
            }
            ContentType.MultiPart.FormData -> {
                val params = call.receiveParameters()
                val dto = CreateDocumentDto(
                    content = params.getOrFail("data"),
                    slug = params["slug"]
                )
                call.createDocument(dto, db, slugGen, reporter)
            }
            else -> {
                println(call.request.contentType())
                val dto = CreateDocumentDto(
                    content = call.receiveText()
                )
                call.createDocument(dto, db, slugGen, reporter)
            }
        }
    }
}

private suspend fun ApplicationCall.createDocument(
    dto: CreateDocumentDto,
    db: TransientEntityStore,
    slugGen: KeyGenerator,
    reporter: StatisticsReporter
) = withContext(Dispatchers.Default) {
    // TODO: uuh yea this ain't too beautiful
    val slugError = if (!dto.slug.isNullOrBlank()) XdDocument.verifySlug(dto.slug) else null
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
        val apiCreds = if (isFrontend) null else apiCredentials(db)
        if (apiCreds != null) {
            GlobalScope.launch {
                reporter.reportEvent(Event.API_KEY_USE, request)
            }
        }
        val usr = apiCreds?.user ?: user(db, !isFrontend)
        val canCreate = apiCreds?.canCreateDocuments ?: true
        // If the user is able to edit documents in general
        val canEdit = apiCreds?.canUpdateDocuments ?: true
        if (dto.slug.isNullOrBlank()) {
            if (!canCreate) {
                HttpStatusCode.Forbidden to CreateDocumentResponseDto(
                    message = "You do not have the permission to create documents"
                )
            } else {
                val isUrl = dto.content.isUrl()
                val slug = slugGen.createKey(db, isUrl)
                val doc = XdDocument.new {
                    this.slug = slug
                    owner = usr
                    stringContent = dto.content.trim()
                    type = if (isUrl) XdDocumentType.URL else XdDocumentType.PASTE
                }

                if (!isUrl) {
                    val id = doc.xdId
                    // Technically always 0
                    val version = doc.version
                    GlobalScope.launch {
                        get<Highlighter>().requestHighlight(id, dto.content, slug, version)
                    }
                }

                GlobalScope.launch {
                    val event = if (isUrl) Event.URL_CREATE else Event.PASTE_CREATE
                    reporter.reportEvent(event, request)
                }
                HttpStatusCode.OK to CreateDocumentResponseDto(
                    isUrl = isUrl,
                    key = doc.slug
                )
            }
        } else {
            // TODO: Properly validate slug and handle failures
            val doc = XdDocument.find(dto.slug)
            if (doc != null) {
                if (!canEdit) {
                    HttpStatusCode.Forbidden to CreateDocumentResponseDto(
                        message = "You do not have the permission to edit documents"
                    )
                } else if (doc.userCanEdit(usr)) {
                    val isUrl = dto.content.isUrl()
                    doc.stringContent = dto.content.trim()
                    doc.type = if (isUrl) XdDocumentType.URL else XdDocumentType.PASTE
                    val version = doc.version++
                    val id = doc.xdId
                    GlobalScope.launch {
                        reporter.reportEvent(Event.DOC_EDIT, request)
                    }
                    GlobalScope.launch {
                        val highlighter = get<Highlighter>()
                        if (!isUrl) {
                            highlighter.requestHighlight(id, dto.content, dto.slug, version)
                        }
                        highlighter.clearCache(id, version)
                    }

                    HttpStatusCode.OK to CreateDocumentResponseDto(
                        isUrl = isUrl,
                        key = doc.slug
                    )
                } else {
                    HttpStatusCode.Conflict to CreateDocumentResponseDto(
                        message = "This URL is already in use, please choose a different one"
                    )
                }
            } else {
                if (!canCreate) {
                    HttpStatusCode.Forbidden to CreateDocumentResponseDto(
                        message = "You do not have the permission to create documents"
                    )
                } else {
                    val isUrl = dto.content.isUrl()
                    val doc = XdDocument.new {
                        slug = dto.slug
                        owner = usr
                        stringContent = dto.content.trim()
                        type = if (isUrl) XdDocumentType.URL else XdDocumentType.PASTE
                    }

                    if (!isUrl) {
                        val id = doc.xdId
                        // Technically always 0
                        val version = doc.version
                        GlobalScope.launch {
                            get<Highlighter>().requestHighlight(id, dto.content, dto.slug, version)
                        }
                    }

                    GlobalScope.launch {
                        val event = if (isUrl) Event.URL_CREATE else Event.PASTE_CREATE
                        reporter.reportEvent(event, request)
                    }
                    HttpStatusCode.OK to CreateDocumentResponseDto(
                        isUrl = isUrl,
                        key = doc.slug
                    )
                }
            }
        }
    }
    respond(result.first, result.second)
}