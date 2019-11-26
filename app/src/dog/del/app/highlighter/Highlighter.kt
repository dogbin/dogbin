package dog.del.app.highlighter

import dog.del.app.config.AppConfig
import dog.del.app.utils.emptyAsNull
import dog.del.commons.ensurePrefix
import dog.del.commons.lineCount
import dog.del.data.base.model.caches.XdHighlighterCache
import dog.del.data.base.model.document.XdDocument
import io.ktor.client.HttpClient
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.url
import io.ktor.client.response.readText
import io.ktor.http.Parameters
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.util.findById
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.Logger

class Highlighter : KoinComponent {
    private val config by inject<AppConfig>()
    private val client by inject<HttpClient>()
    private val store by inject<TransientEntityStore>()
    private val log by inject<Logger>()

    private val highlightJob = SupervisorJob()
    private val highlightContext = Dispatchers.IO + highlightJob + CoroutineName("Highlighter")

    private val cleaningJob = SupervisorJob()
    private val cleaningScope = GlobalScope + cleaningJob + CoroutineName("Highlighter-Cleanup")

    private suspend fun highlight(
        code: String,
        fileName: String? = null,
        language: String? = null
    ): HighlighterResult =
        if (code.lineCount > config.highlighter.maxLines || code.length > config.highlighter.maxChars) {
            HighlighterResult(
                "",
                code,
                ""
            )
        } else withContext(highlightContext) {
            try {
                client.submitForm<HighlighterServiceResult>(
                    formParameters = Parameters.build {
                        if (fileName != null)
                            append("filename", fileName)
                        if (language != null)
                            append("lang", language)
                        append("code", code)
                    }
                ) {
                    url(config.microservices.highlighter)
                }.toHighlighterResult()
            } catch (e: Exception) {
                if (e is ServerResponseException) {
                    log.error(e.response.readText(), e)
                }
                log.error("Highlighter: Highlighting failed", e)
                throw e
            }
        }

    suspend fun highlightDocument(docId: String, rawSlug: String, version: Int, content: String) =
        withContext(Dispatchers.IO) {
            requestHighlight(docId, content, rawSlug, version)
        }

    fun requestHighlight(docId: String, content: String, rawSlug: String, version: Int) =
        if (config.highlighter.skipCache) {
            val lang = rawSlug.substringAfterLast('.', missingDelimiterValue = "").emptyAsNull()
            val fileName = if (rawSlug.contains('.')) rawSlug else null
            runBlocking { highlight(content, fileName, lang) }
        } else try {
            val cached = store.transactional {
                XdHighlighterCache.findOrNew(XdHighlighterCache.filter {
                    it.docId eq docId
                    it.slug eq rawSlug
                    it.version eq version
                    it.highlighterVersion eq HIGHLIGHTER_VERSION
                }) {
                    this.docId = docId
                    this.slug = rawSlug
                    this.version = version
                    val lang = rawSlug.substringAfterLast('.', missingDelimiterValue = "").emptyAsNull()
                    val fileName = if (rawSlug.contains('.')) rawSlug else null
                    val hlResult = runBlocking { highlight(content, fileName, lang) }
                    this.extension = hlResult.extension
                    this.language = hlResult.language
                    this.content = hlResult.content
                    this.highlighterVersion = HIGHLIGHTER_VERSION
                    // in case this request happened without language, or using an alias also ensure the default extension is cached as well
                    val defaultSlug = hlResult.createFilename(rawSlug.substringBeforeLast('.'))
                    if (rawSlug != defaultSlug) {
                        XdHighlighterCache.findOrNew(XdHighlighterCache.filter {
                            it.docId eq docId
                            it.slug eq defaultSlug
                            it.version eq version
                            it.highlighterVersion eq HIGHLIGHTER_VERSION
                        }) {
                            this.docId = docId
                            this.slug = defaultSlug
                            this.version = version
                            this.extension = hlResult.extension
                            this.language = hlResult.language
                            this.content = hlResult.content
                            this.highlighterVersion = HIGHLIGHTER_VERSION
                        }
                    }
                }
            }
            HighlighterResult.fromXdHighlighterCache(store, cached)
        } catch (e: Exception) {
            HighlighterResult("failed", content, ".txt")
        }

    // Clean up cached older versions of documents
    fun clearCache(docId: String, currentVersion: Int) = cleaningScope.launch {
        store.transactional {
            XdHighlighterCache.filter {
                it.docId eq docId
                it.version lt currentVersion
            }.asSequence().forEach {
                it.delete()
            }
        }
    }

    // Cleans up versions cached with an older highlighter version and resubmits them
    fun updateCache() = cleaningScope.launch {
        log.info("Highlighter: Running cache update check for v$HIGHLIGHTER_VERSION")
        store.transactional {
            XdHighlighterCache.filter {
                it.highlighterVersion lt HIGHLIGHTER_VERSION
            }.asSequence().forEach {
                val doc = XdDocument.findById(it.docId)
                // Only resubmit if it's the latest document version (this should always be the case, but let's make sure
                if (it.version == doc.version) {
                    log.info("Highlighter: Updating ${it.slug} from v${it.highlighterVersion}")
                    requestHighlight(it.docId, doc.stringContent!!, it.slug, it.version)
                }
                it.delete()
            }
        }
    }

    data class HighlighterResult(
        private val lang: String,
        val content: String,
        private val ext: String
    ) {
        val language = if (lang == "fallback") "" else lang
        val extension = if (lang == "fallback") ".txt" else ext
        fun createFilename(slug: String) = slug + extension

        companion object {
            internal fun fromXdHighlighterCache(store: TransientEntityStore, xdHighlighterCache: XdHighlighterCache) =
                store.transactional(readonly = true) {
                    HighlighterResult(
                        xdHighlighterCache.language.orEmpty(),
                        xdHighlighterCache.content,
                        xdHighlighterCache.extension.orEmpty()
                    )
                }
        }
    }

    data class HighlighterServiceResult(
        val lang: String,
        val code: String,
        val filenames: List<String>
    ) {
        private val extension
            get() = (filenames.firstOrNull()?.substringAfter('.') ?: lang.replace(" ", "")).ensurePrefix('.')

        fun toHighlighterResult() = HighlighterResult(
            lang,
            code,
            extension
        )
    }

    companion object {
        const val HIGHLIGHTER_VERSION = 2
    }
}