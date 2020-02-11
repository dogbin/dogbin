package dog.del.app.screenshotter

import dog.del.app.config.AppConfig
import dog.del.data.base.model.caches.XdScreenshotCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class Screenshotter : KoinComponent {
    private val store by inject<TransientEntityStore>()
    private val client by inject<HttpClient>()
    private val config by inject<AppConfig>()

    suspend fun getScreenshotUrl(path: String, version: Int): String? =
        if (!config.screenshotter.enabled) null else store.transactional {
            val entry = XdScreenshotCache.find(path)
            if (entry == null) {
                val screenshotUrl = runBlocking { captureScreenshot(path) }
                XdScreenshotCache.new {
                    this.path = path
                    this.version = version
                    this.screenshotUrl = screenshotUrl
                }
                return@transactional screenshotUrl
            }
            if (entry.version < version) {
                entry.screenshotUrl = runBlocking { captureScreenshot(path) }
                entry.version = version
            }
            return@transactional entry.screenshotUrl
        }

    private suspend fun captureScreenshot(path: String): String? = withContext(Dispatchers.IO) {
        if (config.screenshotter.enabled) client.get<String>("${config.microservices.screenshotter}/$path") else null
    }
}