package dog.del.app.stats

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.annotations.SerializedName
import dog.del.app.config.AppConfig
import dog.del.app.metrics.DogbinMetrics
import dog.del.app.utils.*
import dog.del.commons.date
import dog.del.commons.format
import dog.del.data.base.model.document.XdDocument
import io.ktor.client.HttpClient
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.features.origin
import io.ktor.http.URLProtocol
import io.ktor.request.*
import io.ktor.util.url
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

class SimpleAnalyticsReporter : StatisticsReporter, KoinComponent {
    private val client by inject<HttpClient>()
    private val db by inject<TransientEntityStore>()
    private val log by inject<Logger>()
    private val config by inject<AppConfig>()
    private val metrics by inject<DogbinMetrics>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val timezoneCache = Caffeine.newBuilder()
        .maximumSize(100)
        .recordStats()
        .buildAsync<String, String?> { slug, _ ->
            scope.future {
                getTimezone(slug)
            }
        }.also {
            metrics.cacheMetrics.addCache("analyticsTimezoneCache", it)
        }

    override val showCount = false
    override val embedCode = """
        <script async defer src="${GhostBuster.bust("/static/SA.min.js")}"></script>
        <noscript><img src="https://api.simpleanalytics.io/hello.gif" alt=""></noscript>
    """.trimIndent()

    override fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
        // Reporting of frontend impressions is handled by the embedded script
        if (!frontend) {
            // only get timezone for IP if dnt is disabled
            val tz = if (!request.dnt) timezoneCache[request.origin.remoteHost].asDeferred() else null
            val url = url {
                protocol = URLProtocol.createOrDefault(request.origin.scheme)
                host = request.host()
                path(slug)
            }
            scope.launch {
                client.post("https://api.simpleanalytics.io/post") {
                    body = defaultSerializer().write(
                        ImpressionRequest(
                            url = url,
                            timezone = tz?.await(),
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

    override suspend fun getImpressions(slug: String): Int = withContext(scope.coroutineContext) {
        try {
            client.get<StatsResult>("https://simpleanalytics.com/${config.host}/$slug.json").pageviews
        } catch (e: Exception) {
            log.error("Error while trying to get stats from SA for $slug", e)
            -1
        }
    }

    // TODO: report with past events?
    override fun reportEvent(event: StatisticsReporter.Event, request: ApplicationRequest) {
        scope.launch {
            client.post("https://api.simpleanalytics.io/events") {
                body = defaultSerializer().write(
                    EventData(
                        ua = request.userAgent(),
                        hostname = request.origin.host,
                        ref = request.refs,
                        date = date().format("yyyy-MM-dd"),
                        events = listOf(event.name)
                    )
                )
            }
        }
    }

    override fun getUrl(slug: String) = "https://simpleanalytics.com/${config.host}/$slug"

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

    // See: https://docs.simpleanalytics.com/events/ios,
    // https://github.com/simpleanalytics/scripts/blob/master/src/external.js,
    // https://github.com/simpleanalytics/scripts/blob/master/src/iframe.js
    data class EventData(
        val ua: String?,
        val hostname: String,
        val date: String,
        val ref: String?,
        val events: List<String>,
        val v: Int = 1
    )

    data class StatsResult(
        val pageviews: Int
    )
}