package dog.del.app.markdown.iframely

import com.google.gson.annotations.SerializedName
import dog.del.app.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.Logger
import java.lang.Exception

class Iframely: KoinComponent {
    private val config by inject<AppConfig>()
    private val client by inject<HttpClient>()
    private val log by inject<Logger>()

    suspend fun getEmbed(url: String): OembedResponse? = withContext(Dispatchers.IO) {
        try {
            client.get<OembedResponse>("${config.microservices.iframely}/oembed") {
                parameter("url", url)
            }
        } catch (e: Exception) {
            log.error("Failed to fetch embedding info for \"$url\"", e)
            null
        }
    }

    data class OembedResponse(
        val title: String,
        val description: String?,
        val type: String,
        @SerializedName("thumbnail_url")
        val thumbnailUrl: String?,
        val url: String,
        val html: String?,
        @SerializedName("provider_name")
        val providerName: String?
    )
}