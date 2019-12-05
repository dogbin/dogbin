package dog.del.app.stats

import dog.del.app.config.AppConfig
import dog.del.app.utils.clientHash
import dog.del.app.utils.isBot
import io.ktor.request.ApplicationRequest
import net.jodah.expiringmap.ExpiringMap
import java.util.concurrent.TimeUnit

interface StatisticsReporter {
    companion object {
        fun getReporter(config: AppConfig) = when {
            !config.stats.enabled -> NopReporter()
            config.stats.useSA -> ReporterWrapper(SimpleAnalyticsReporter())
            else -> ReporterWrapper(DBStatisticsReporter())
        }
    }

    val showCount: Boolean
    val embedCode: String
    fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest)
    suspend fun getImpressions(slug: String): Int
    fun reportEvent(event: Event, request: ApplicationRequest)
    /**
     * Get a publicly accessible url with statistics for the supplied slug
     */
    fun getUrl(slug: String): String?

    class NopReporter : StatisticsReporter {
        override val showCount = false
        override val embedCode = ""

        override fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
            // nop
        }

        // -1 hides the view count on the frontend
        override suspend fun getImpressions(slug: String) = -1

        override fun reportEvent(event: Event, request: ApplicationRequest) {
            // nop
        }

        override fun getUrl(slug: String): String? = null
    }

    class ReporterWrapper(private val reporter: StatisticsReporter) : StatisticsReporter {
        override val showCount = reporter.showCount
        override val embedCode = reporter.embedCode

        private val impressionCache = ExpiringMap.builder()
            .maxSize(1000)
            .expiration(10, TimeUnit.MINUTES)
            .build<String, Boolean>()

        override fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
            // Ignore bot impressions
            if (!request.isBot) {
                // Ignore repeated impressions within 10 minutes
                val reqKey = "${request.clientHash}:$slug"
                if (impressionCache.containsKey(reqKey)) {
                    return
                }

                reporter.reportImpression(slug, frontend, request)
                impressionCache[reqKey] = true
            }
        }

        override suspend fun getImpressions(slug: String) =
            reporter.getImpressions(slug)

        override fun reportEvent(event: Event, request: ApplicationRequest) {
            // Ignore bot events
            if (!request.isBot) {
                reporter.reportEvent(event, request)
            }
        }

        override fun getUrl(slug: String) = reporter.getUrl(slug)
    }

    enum class Event {
        USER_REGISTER,
        PASTE_CREATE,
        DOC_EDIT,
        URL_CREATE,
        URL_REDIRECT,
        API_KEY_CREATE,
        API_KEY_USE,
        API_KEY_DELETE,
    }
}