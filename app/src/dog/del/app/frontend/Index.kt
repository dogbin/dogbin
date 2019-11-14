package dog.del.app.frontend

import dog.del.app.dto.FrontendDocumentDto
import dog.del.app.highlighter.Highlighter
import dog.del.app.session.session
import dog.del.app.session.user
import dog.del.app.stats.StatisticsReporter
import dog.del.app.stats.StatisticsReporter.*
import dog.del.app.utils.locale
import dog.del.app.utils.rawSlug
import dog.del.app.utils.slug
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import io.ktor.application.call
import io.ktor.pebble.respondTemplate
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import org.koin.ktor.ext.inject

fun Route.index() = route("/") {
    val store by inject<TransientEntityStore>()
    val reporter by inject<StatisticsReporter>()
    val highlighter by inject<Highlighter>()

    get {
        call.respondTemplate(
            "index", mapOf(
                "title" to "dogbin"
            )
        )
    }

    get("/{slug}") {
        var documentDto: FrontendDocumentDto? = null
        var editable = false
        var highlightDeferred: Deferred<Highlighter.HighlighterResult>? = null
        store.transactional(readonly = true) {
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
                    val slug = doc.slug
                    GlobalScope.launch {
                        reporter.reportImpression(slug, false, call.request)
                        reporter.reportEvent(Event.URL_REDIRECT, call.request)
                    }
                } else {
                    highlightDeferred = async {
                        highlighter.highlightDocument(
                            doc.xdId,
                            call.rawSlug,
                            doc.version,
                            doc.stringContent!!
                        )
                    }
                    documentDto = FrontendDocumentDto.fromDocument(doc, reporter, call.locale)
                    if (call.session() != null) {
                        val usr = call.user(store)
                        editable = doc.userCanEdit(usr)
                    }
                }
            }
            Unit
        }
        if (documentDto != null) {
            val highlightResult = highlightDeferred?.await()
            val hlSlug = highlightResult?.createFilename(call.slug)
            if (hlSlug != null && hlSlug != call.rawSlug) {
                call.respondRedirect("/$hlSlug")
                return@get
            }
            documentDto!!.rendered = highlightResult?.content
            call.respondTemplate(
                "index", mapOf(
                    "title" to documentDto!!.title,
                    "description" to documentDto!!.description,
                    "document" to documentDto!!,
                    "editable" to editable
                )
            )
            GlobalScope.launch {
                reporter.reportImpression(documentDto!!.slug, true, call.request)
            }
        }
    }

    get("/v/{slug}") {
        var documentDto: FrontendDocumentDto? = null
        var editable = false
        var highlightDeferred: Deferred<Highlighter.HighlighterResult?>? = null
        store.transactional(readonly = true) {
            val doc = XdDocument.find(call.slug)
            if (doc == null) {
                runBlocking {
                    call.respondRedirect("/")
                }
            } else {
                highlightDeferred = async {
                    if (doc.type == XdDocumentType.PASTE) {
                        highlighter.highlightDocument(
                            doc.xdId,
                            call.rawSlug,
                            doc.version,
                            doc.stringContent!!
                        )
                    } else null
                }
                documentDto = FrontendDocumentDto.fromDocument(doc, reporter, call.locale)
                if (call.session() != null) {
                    val usr = call.user(store)
                    editable = doc.userCanEdit(usr)
                }
            }
        }
        if (documentDto != null) {
            val highlightResult = highlightDeferred?.await()
            documentDto!!.rendered = highlightResult?.content
            call.respondTemplate(
                "index", mapOf(
                    "title" to documentDto!!.title,
                    "description" to documentDto!!.description,
                    "document" to documentDto!!,
                    "editable" to editable
                )
            )
            GlobalScope.launch {
                reporter.reportImpression(documentDto!!.slug, true, call.request)
            }
        }
    }

    get("/e/{slug}") {
        var documentDto: FrontendDocumentDto? = null
        var canEdit = false
        store.transactional(readonly = true) {
            val doc = XdDocument.find(call.slug)
            if (doc == null) {
                runBlocking {
                    call.respondRedirect("/")
                }
            } else {
                documentDto = FrontendDocumentDto.fromDocument(doc, reporter, call.locale)
                if (call.session() != null) {
                    val usr = call.user(store)
                    canEdit = doc.userCanEdit(usr)
                }
            }
        }
        if (!canEdit) {
            call.respondRedirect("/")
            return@get
        }
        if (documentDto != null) {
            call.respondTemplate(
                "index", mapOf(
                    "title" to "Editing - ${documentDto!!.title}",
                    "description" to documentDto!!.description,
                    "editKey" to call.slug,
                    "initialValue" to (documentDto!!.content ?: "")
                )
            )
        }
    }
}