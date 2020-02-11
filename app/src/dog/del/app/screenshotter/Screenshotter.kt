package dog.del.app.screenshotter

import dog.del.app.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class Screenshotter : KoinComponent {
    private val client by inject<HttpClient>()
    private val config by inject<AppConfig>()

    suspend fun getScreenshotUrl(path: String, version: Int): String? = withContext(Dispatchers.IO) {
        if (config.screenshotter.enabled) try {
            client.get<String>("${config.microservices.screenshotter}/$path?v=$version")
        } catch (e: Exception) {
            null
        } else null
    }
}