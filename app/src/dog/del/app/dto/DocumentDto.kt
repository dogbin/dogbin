package dog.del.app.dto

import dog.del.app.highlighter.Highlighter
import dog.del.app.markdown.MarkdownRenderer
import dog.del.app.screenshotter.Screenshotter
import dog.del.app.session.session
import dog.del.app.session.user
import dog.del.app.stats.StatisticsReporter
import dog.del.app.utils.PebbleModel
import dog.del.app.utils.locale
import dog.del.app.utils.rawSlug
import dog.del.commons.formatShort
import dog.del.commons.lineCount
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import io.ktor.application.ApplicationCall
import io.ktor.request.path
import io.ktor.response.respondRedirect
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.inject

open class FrontendDocumentDto : KoinComponent, PebbleModel {
    protected val store by inject<TransientEntityStore>()
    protected val reporter by inject<StatisticsReporter>()
    protected val screenshotter by inject<Screenshotter>()

    var slug: String = ""
        protected set

    var type: DocumentTypeDto = DocumentTypeDto.PASTE
        protected set

    protected var contentDeferred: Deferred<String?>? = null

    open var content: String? = null
        protected set
        get() = if (contentDeferred != null) runBlocking { contentDeferred!!.await() } else field

    var owner: UserDto? = null
        protected set

    var created: String = ""
        protected set

    protected var version: Int = 0

    protected var screenshotDeferred: Deferred<String?>? = null
    var screenshotUrl: String? = null
        get() = runBlocking { screenshotDeferred?.await() }

    protected var viewCountDeferred: Deferred<Int>? = null
    var viewCount: Int = -1
        protected set
        get() = if (viewCountDeferred != null) {
            runBlocking { viewCountDeferred!!.await() }
        } else field

    open var redirectTo: String? = null
        protected set

    var editable = false
        protected set

    open val showLines = true
    open val rendered = false
    val viewUrl: String get() = if (type == DocumentTypeDto.URL) "/v/$slug" else "/$slug"
    val statsUrl: String? get() = reporter.getUrl(slug)
    val showCount: Boolean get() = reporter.showCount
    val lines get() = content?.lineCount ?: 0
    var description = "The sexiest pastebin and url-shortener ever"
        protected set
    var title = "dogbin"
        protected set
    open val editing = false
    protected var docContent: String? = null

    open suspend fun applyFrom(document: XdDocument, call: ApplicationCall? = null): FrontendDocumentDto =
        coroutineScope {
            slug = store.transactional(readonly = true) { document.slug }
            version = store.transactional(readonly = true) { document.version }
            title = "dogbin - $slug"
            if (reporter.showCount) {
                viewCountDeferred = async { reporter.getImpressions(slug) }
            }
            screenshotDeferred = async { screenshotter.getScreenshotUrl(call?.request?.path() ?: slug, version) }
            store.transactional(readonly = true) {
                type = DocumentTypeDto.fromXdDocumentType(document.type)
                docContent = document.stringContent
                description = docContent?.take(100) ?: description
                content = docContent
                owner = UserDto.fromUser(document.owner)
                created = document.created.formatShort(call?.locale)
                if (call?.session() != null) {
                    val usr = call.user(store)
                    editable = document.userCanEdit(usr)
                }
            }
            return@coroutineScope this@FrontendDocumentDto
        }

    override suspend fun onRespond(call: ApplicationCall): Boolean {
        if (redirectTo != null) {
            call.respondRedirect(redirectTo!!)
            return true
        }
        return false
    }

    override suspend fun toModel(): Map<String, Any> = mapOf(
        "title" to title,
        "description" to description,
        "document" to this
    )
}

// TODO: use front matter for description and title
class MarkdownDocumentDto : FrontendDocumentDto() {
    private val mdRenderer by inject<MarkdownRenderer>()

    override val showLines = false
    override val rendered = true

    override suspend fun applyFrom(document: XdDocument, call: ApplicationCall?): FrontendDocumentDto {
        super.applyFrom(document, call)

        if (docContent != null) {
            val mdContent = mdRenderer.render(docContent!!)
            title = mdContent.title ?: title
            description = mdContent.description ?: description
            content = mdContent.content
        }
        return this
    }
}

class HighlightedDocumentDto(private val redirectToFull: Boolean = true) : FrontendDocumentDto() {
    private val highlighter by inject<Highlighter>()

    private var rawSlug: String = ""
    private var highlightDeferred: Deferred<Highlighter.HighlighterResult>? = null
    override var content: String?
        get() = runBlocking { highlightDeferred?.await()?.content }
        set(value) {}
    override val rendered get() = runBlocking { (highlightDeferred?.await()?.language ?: "failed") != "failed" }
    override var redirectTo: String?
        get() = if (!redirectToFull) null else runBlocking {
            val res = highlightDeferred?.await()
            val hlSlug = res?.createFilename(slug)
            if (hlSlug != null && hlSlug != rawSlug) {
                "/$hlSlug"
            } else null
        }
        set(value) {}

    override suspend fun applyFrom(document: XdDocument, call: ApplicationCall?): FrontendDocumentDto = coroutineScope {
        super.applyFrom(document, call)

        val docId = store.transactional(readonly = true) { document.xdId }
        val version = store.transactional(readonly = true) { document.version }

        rawSlug = call?.rawSlug ?: slug
        highlightDeferred = async {
            highlighter.highlightDocument(
                docId,
                rawSlug,
                version,
                docContent!!
            )
        }

        screenshotDeferred =
            async { screenshotter.getScreenshotUrl(redirectTo ?: call?.request?.path() ?: slug, version) }

        return@coroutineScope this@HighlightedDocumentDto
    }
}

class EditDocumentDto : FrontendDocumentDto() {
    override var redirectTo: String?
        get() = if (!editable) viewUrl else null
        set(value) {}
    override val editing = true

    override suspend fun applyFrom(document: XdDocument, call: ApplicationCall?): FrontendDocumentDto {
        super.applyFrom(document, call)

        title = "Editing - $slug"

        screenshotDeferred = null

        return this
    }

    override suspend fun toModel(): Map<String, Any> {
        return super.toModel() + mapOf(
            "initialValue" to (content ?: ""),
            "editKey" to slug
        )
    }
}

data class CreateDocumentDto(
    val content: String,
    val slug: String? = null
)

data class CreateDocumentResponseDto(
    val isUrl: Boolean? = null,
    val key: String? = null,
    val message: String? = null
)

enum class DocumentTypeDto {
    URL, PASTE;

    companion object {
        fun fromXdDocumentType(documentType: XdDocumentType) = when (documentType) {
            XdDocumentType.PASTE -> PASTE
            XdDocumentType.URL -> URL
            else -> throw IllegalStateException()
        }
    }
}