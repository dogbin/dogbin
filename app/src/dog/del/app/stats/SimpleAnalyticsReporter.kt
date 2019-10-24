package dog.del.app.stats

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.annotations.SerializedName
import dog.del.app.config.AppConfig
import dog.del.app.utils.dnt
import dog.del.app.utils.emptyAsNull
import dog.del.app.utils.referer
import dog.del.app.utils.refs
import dog.del.data.base.model.document.XdDocument
import io.ktor.client.HttpClient
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.features.origin
import io.ktor.http.URLProtocol
import io.ktor.request.*
import io.ktor.util.url
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

// TODO: Modify embedded js to strip /v/ and file extensions
// TODO: Add events code
class SimpleAnalyticsReporter : StatisticsReporter, KoinComponent {
    private val client by inject<HttpClient>()
    private val db by inject<TransientEntityStore>()
    private val log by inject<Logger>()
    private val config by inject<AppConfig>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val timezoneCache = Caffeine.newBuilder()
        .maximumSize(100)
        .buildAsync<String, String?> { slug, _ ->
            scope.future {
                getTimezone(slug)
            }
        }
    private val impressionsCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .buildAsync<String, Int> { slug, _ ->
            scope.future {
                getSaViewCount(slug)
            }
        }

    override val embedCode = """
        <script async defer src="https://cdn.simpleanalytics.io/hello.js"></script>
        <noscript><img src="https://api.simpleanalytics.io/hello.gif" alt=""></noscript>
    """.trimIndent()

    override suspend fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
        // Reporting of frontend impressions is handled by the embedded script
        if (!frontend) {
            // only get timezone for IP if dnt is disabled
            val tz = if (!request.dnt) timezoneCache[request.origin.remoteHost].asDeferred().await() else null
            val url = url {
                protocol = URLProtocol.createOrDefault(request.origin.scheme)
                host = request.host()
                path(slug)
            }.toString()
            scope.launch {
                client.post("https://api.simpleanalytics.io/post") {
                    body = defaultSerializer().write(
                        ImpressionRequest(
                            url = url,
                            timezone = tz,
                            referrer = request.referer,
                            urlReferrer = request.refs,
                            // No way for us to know
                            width = null,
                            ua = request.userAgent()
                        )
                    )
                }
            }
        }
    }

    // Get impressions from SA in an async manner and store to db to avoid network requests in hot-path as good as possible
    override suspend fun getImpressions(slug: String): Int = withContext(scope.coroutineContext) {
        // Get latest value from in memory cache
        val saFuture = impressionsCache.getIfPresent(slug)
        if (saFuture != null) {
            val saValue = saFuture.asDeferred().await()
            if (saValue != -1) {
                return@withContext saValue
            }
        } else {
            // Update stored value
            GlobalScope.launch {
                val newValue = impressionsCache.get(slug).asDeferred().await()
                db.transactional {
                    XdDocument.find(slug)?.apply {
                        viewCount = newValue
                    }
                }
            }
        }
        // Get value that was last written to the db
        return@withContext db.transactional {
            XdDocument.find(slug)?.viewCount ?: 0
        }
    }

    override suspend fun reportEvent(event: StatisticsReporter.Event, request: ApplicationRequest) {
        scope.launch {

        }
    }

    private suspend fun getSaViewCount(slug: String): Int {
        try {
            return client.request<StatsResult>("https://simpleanalytics.com/${config.host}/$slug.json") {}.pageviews
        } catch (e: Exception) {
            log.error("Error while trying to get stats from SA", e)
        }
        return -1
    }

    private suspend fun getTimezone(ip: String): String? {
        try {
            return client.get<GeoipResult>("https://freegeoip.app/json/$ip").timezone.emptyAsNull()
        } catch (e: Exception) {
            log.error("Error while trying to get timezone", e)
        }
        return null
    }

    data class GeoipResult(
        @SerializedName("time_zone")
        val timezone: String?
    )

    data class ImpressionRequest(
        val url: String,
        val timezone: String?,
        val referrer: String?,
        val urlReferrer: String?,
        val width: Int?,
        val ua: String?
    )

    data class StatsResult(
        val pageviews: Int
    )
}