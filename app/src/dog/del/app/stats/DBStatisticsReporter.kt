package dog.del.app.stats

import dog.del.app.utils.isBot
import dog.del.data.base.model.document.XdDocument
import io.ktor.request.ApplicationRequest
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class DBStatisticsReporter : StatisticsReporter, KoinComponent {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val db by inject<TransientEntityStore>()

    override val embedCode = ""

    override suspend fun reportImpression(slug: String, frontend: Boolean, request: ApplicationRequest) {
        scope.launch {
            db.transactional {
                XdDocument.find(slug)?.apply {
                    viewCount++
                }
            }
        }
    }

    override suspend fun getImpressions(slug: String): Int = db.transactional {
        XdDocument.find(slug)?.viewCount ?: 0
    }

    override suspend fun reportEvent(event: StatisticsReporter.Event, request: ApplicationRequest) {
        // Not implemented for our basic db reporter
    }
}