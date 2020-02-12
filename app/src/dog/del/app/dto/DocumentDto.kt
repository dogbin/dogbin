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
import dog.del.data.base.DB
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.suspended
import io.ktor.application.ApplicationCall
import io.ktor.request.path
import io.ktor.response.respondRedirect
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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

    protected var version = 0
        private set

    open var content: String? = null
        protected set

    var owner: UserDto? = null
        protected set

    var created: String = ""
        protected set

    var screenshotUrl: String? = null
        protected set

    var viewCount: Int = -1
        protected set

    open var redirectTo: String? = null
        protected set

    var editable = false
        protected set

    private val loadScreenshot: Boolean
        get() = !editing

    open val showLines = true
    open var rendered = false
        protected set
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
            store.suspended(readonly = true) {
                slug = document.slug
                version = document.version
            }
            title = "dogbin - $slug"
            if (reporter.showCount) {
                viewCount = reporter.getImpressions(slug)
            }
            if (loadScreenshot) {
                screenshotUrl = screenshotter.getScreenshotUrl(call?.request?.path() ?: slug, version)
            }
            val usr = if (call?.session() != null) {
                call.user(store)
            } else null
            store.suspended(readonly = true) {
                type = DocumentTypeDto.fromXdDocumentType(document.type)
                docContent = document.stringContent
                description = docContent?.take(100) ?: description
                content = docContent
                owner = UserDto.fromUser(document.owner)
                created = document.created.formatShort(call?.locale)
                if (usr != null) {
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
    override var rendered = true

    override suspend fun applyFrom(document: XdDocument, call: ApplicationCall?): FrontendDocumentDto = coroutineScope {
        super.applyFrom(document, call)

        if (docContent != null) {
            val mdContent = mdRenderer.render(docContent!!)
            title = mdContent.title ?: title
            description = mdContent.description ?: description
            content = mdContent.content
        }
        return@coroutineScope this@MarkdownDocumentDto
    }
}

class HighlightedDocumentDto(private val redirectToFull: Boolean = true) : FrontendDocumentDto() {
    private val highlighter by inject<Highlighter>()

    override suspend fun applyFrom(document: XdDocument, call: ApplicationCall?): FrontendDocumentDto =
        coroutineScope {
            super.applyFrom(document, call)

            val docId = store.suspended(readonly = true) { document.xdId }

            val rawSlug = call?.rawSlug ?: slug
            val highlighted = try {
                highlighter.highlightDocument(
                    docId,
                    rawSlug,
                    version,
                    docContent!!
                )
            } catch (e: Exception) {
                null
            }
            rendered = highlighted != null && highlighted.language != "failed"
            if (rendered) {
                content = highlighted!!.content
                if (redirectToFull) {
                    val hlSlug = highlighted!!.createFilename(slug)
                    if (hlSlug != rawSlug) {
                        redirectTo = "/$hlSlug"
                    }
                }
            }

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