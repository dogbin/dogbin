package dog.del.app.frontend

import dog.del.app.dto.EditDocumentDto
import dog.del.app.dto.FrontendDocumentDto
import dog.del.app.dto.HighlightedDocumentDto
import dog.del.app.dto.MarkdownDocumentDto
import dog.del.app.highlighter.Highlighter
import dog.del.app.markdown.MarkdownRenderer
import dog.del.app.session.session
import dog.del.app.session.user
import dog.del.app.stats.StatisticsReporter
import dog.del.app.stats.StatisticsReporter.*
import dog.del.app.utils.*
import dog.del.data.base.Database
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.suspended
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

    get {
        call.respondTemplate(
            "index", mapOf(
                "title" to "dogbin"
            )
        )
    }

    get("/{slug}") {
        var doc: XdDocument? = null
        var isUrl = false
        var docContent: String? = null
        store.suspended {
            doc = XdDocument.find(call.slug)
            if (doc != null) {
                isUrl = doc!!.type == XdDocumentType.URL
                if (isUrl) {
                    docContent = doc!!.stringContent
                }
            }
        }

        if (doc == null) {
            call.respondRedirect("/")
            return@get
        }

        if (isUrl) {
            call.respondRedirect(docContent!!, true)
            GlobalScope.launch {
                reporter.reportImpression(call.slug, false, call.request)
                reporter.reportEvent(Event.URL_REDIRECT, call.request)
            }
            return@get
        }

        val dto = if (call.hlLang == "md") {
            MarkdownDocumentDto().applyFrom(doc!!, call)
        } else {
            HighlightedDocumentDto().applyFrom(doc!!, call)
        }
        call.respondTemplate("index", dto)
        GlobalScope.launch {
            reporter.reportImpression(dto.slug, true, call.request)
        }
    }

    get("/v/{slug}") {
        var isUrl = false
        var doc: XdDocument? = null
        store.suspended(readonly = true) {
            doc = XdDocument.find(call.slug)
            if (doc != null) {
                isUrl = doc!!.type == XdDocumentType.URL
            }
        }
        if (doc == null) {
            call.respondRedirect("/")
            return@get
        }

        val dto = if (isUrl) {
            FrontendDocumentDto().applyFrom(doc!!, call)
        } else {
            HighlightedDocumentDto(false).applyFrom(doc!!, call)
        }

        call.respondTemplate("index", dto)
        GlobalScope.launch {
            reporter.reportImpression(dto.slug, true, call.request)
        }
    }

    get("/e/{slug}") {
        val doc = store.suspended(readonly = true) {
            XdDocument.find(call.slug)
        }
        if (doc == null) {
            call.respondRedirect("/")
            return@get
        }
        val dto = EditDocumentDto().applyFrom(doc, call)
        call.respondTemplate("index", dto)
    }
}