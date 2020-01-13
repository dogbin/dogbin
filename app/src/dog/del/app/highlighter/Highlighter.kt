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
            val lang = rawSlug.substringAfterLast('.', missingDelimiterValue = "").emptyAsNull()
            val fileName = if (rawSlug.contains('.')) rawSlug else null
            highlight(content, fileName, lang)
        }

    data class HighlighterResult(
        private val lang: String,
        val content: String,
        private val ext: String
    ) {
        val language = if (lang == "fallback") "" else lang
        val extension = if (lang == "fallback") ".txt" else ext
        fun createFilename(slug: String) = slug + extension
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