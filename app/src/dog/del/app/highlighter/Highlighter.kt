package dog.del.app.highlighter

import dog.del.app.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.url
import io.ktor.client.response.readText
import io.ktor.http.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.lang.Exception

class Highlighter : KoinComponent {
    private val config by inject<AppConfig>()
    private val client by inject<HttpClient>()

    suspend fun highlight(code: String?, fileName: String? = null, language: String? = null): String? = withContext(Dispatchers.IO){
        if (code.isNullOrBlank()) return@withContext ""
        try {
            client.submitForm<HighlighterResult>(
                formParameters = Parameters.build {
                    if (fileName != null)
                        append("filename", fileName)
                    if (language != null)
                        append("lang", language)
                    append("code", code)
                }
            ) {
                url(config.microservices.highlighter)
            }.code
        } catch (e: Exception) {
            // TODO: switch to logger
            if (e is ServerResponseException) {
                println(e.response.readText())
            }
            e.printStackTrace()
            return@withContext null
        }
    }

    data class HighlighterResult(
        val lang: String,
        val code: String
    )
}