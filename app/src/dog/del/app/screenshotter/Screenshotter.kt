package dog.del.app.screenshotter

import dog.del.app.config.AppConfig
import dog.del.data.base.model.caches.XdScreenshotCache
import dog.del.data.base.suspended
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class Screenshotter : KoinComponent {
    private val client by inject<HttpClient>()
    private val config by inject<AppConfig>()
    private val db by inject<TransientEntityStore>()

    suspend fun getScreenshotUrl(path: String, version: Int): String? = withContext(Dispatchers.IO) {
        if (config.screenshotter.enabled) try {
            val screenshotCache = db.suspended(true) {
                XdScreenshotCache.find(path)
            }
            if (screenshotCache == null || db.suspended(true) { screenshotCache.version < version }) {
                val url = client.get<String>("${config.microservices.screenshotter}/$path?v=$version")
                if (screenshotCache != null) {
                    db.suspended {
                        screenshotCache.screenshotUrl = url
                        screenshotCache.version = version
                    }
                } else {
                    db.suspended {
                        XdScreenshotCache.new {
                            this.path = path
                            this.screenshotUrl = url
                            this.version = version
                        }
                    }
                }
                return@withContext url
            }
            db.suspended(true) { screenshotCache.screenshotUrl }
        } catch (e: Exception) {
            println(e)
            null
        } else null
    }
}