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

    val embedCode: String
    suspend fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest)
    suspend fun getImpressions(slug: String): Int
    suspend fun reportEvent(event: Event, request: ApplicationRequest)


    class NopReporter : StatisticsReporter {
        override val embedCode = ""

        override suspend fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
            // nop
        }

        // -1 hides the view count on the frontend
        override suspend fun getImpressions(slug: String) = -1

        override suspend fun reportEvent(event: Event, request: ApplicationRequest) {
            // nop
        }
    }

    class ReporterWrapper(private val reporter: StatisticsReporter) : StatisticsReporter {
        override val embedCode = reporter.embedCode

        private val impressionCache = ExpiringMap.builder()
            .maxSize(1000)
            .expiration(10, TimeUnit.MINUTES)
            .build<String, Boolean>()

        override suspend fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
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

        override suspend fun reportEvent(event: Event, request: ApplicationRequest) {
            // Ignore bot events
            if (!request.isBot) {
                reporter.reportEvent(event, request)
            }
        }
    }

    enum class Event {
        USER_REGISTER,
        PASTE_CREATE,
        DOC_EDIT,
        URL_CREATE,
        URL_REDIRECT,
    }
}