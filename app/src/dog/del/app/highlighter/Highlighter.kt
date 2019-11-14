package dog.del.app.highlighter

import dog.del.app.config.AppConfig
import dog.del.app.utils.emptyAsNull
import dog.del.commons.ensurePrefix
import dog.del.data.base.model.caches.XdHighlighterCache
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
import org.koin.core.KoinComponent
import org.koin.core.inject

class Highlighter : KoinComponent {
    private val config by inject<AppConfig>()
    private val client by inject<HttpClient>()
    private val store by inject<TransientEntityStore>()

    private val highlightJob = SupervisorJob()
    private val highlightContext = Dispatchers.IO + highlightJob + CoroutineName("Highlighter")

    private val cleaningJob = SupervisorJob()
    private val cleaningScope = GlobalScope + cleaningJob + CoroutineName("Highlighter-Cleanup")

    private suspend fun highlight(
        code: String?,
        fileName: String? = null,
        language: String? = null
    ): HighlighterResult? =
        withContext(highlightContext) {
            if (code.isNullOrBlank()) return@withContext null
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
                // TODO: switch to logger
                if (e is ServerResponseException) {
                    println(e.response.readText())
                }
                e.printStackTrace()
                return@withContext null
            }
        }

    suspend fun highlightDocument(docId: String, rawSlug: String, version: Int, content: String) =
        withContext(Dispatchers.IO) {
            val cached = requestHighlight(docId, content, rawSlug, version)
            HighlighterResult.fromXdHighlighterCache(store, cached)
        }

    fun requestHighlight(docId: String, content: String, rawSlug: String, version: Int) = store.transactional {
        XdHighlighterCache.findOrNew(XdHighlighterCache.filter {
            it.docId eq docId
            it.slug eq rawSlug
            it.version eq version
        }) {
            this.docId = docId
            this.slug = rawSlug
            this.version = version
            val lang = rawSlug.substringAfterLast('.', missingDelimiterValue = "").emptyAsNull()
            val fileName = if (rawSlug.contains('.')) rawSlug else null
            val hlResult = runBlocking { highlight(content, fileName, lang) }
            if (hlResult != null) {
                if (hlResult.language == "fallback") {
                    language = ""
                    extension = ".txt"
                } else {
                    extension = hlResult.extension
                    language = hlResult.language
                }
                this.content = hlResult.content
            } else {
                this.content = content
                language = ""
                extension = ".txt"
            }
        }
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

    data class HighlighterResult(
        val language: String,
        val content: String,
        val extension: String
    ) {
        val isMarkdown = language.toLowerCase() == "markdown"
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
}