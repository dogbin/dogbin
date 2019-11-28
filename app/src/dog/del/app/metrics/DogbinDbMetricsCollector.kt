package dog.del.app.metrics

import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.model.session.XdSession
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.model.user.XdUserRole
import io.prometheus.client.Collector
import io.prometheus.client.CounterMetricFamily
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.roughSize
import org.koin.core.KoinComponent
import org.koin.core.inject

class DogbinDbMetricsCollector : Collector(), KoinComponent {
    private val store by inject<TransientEntityStore>()

    override fun collect(): MutableList<MetricFamilySamples> = store.transactional {
        mutableListOf(
            CounterMetricFamily(
                "dogbin_db_documents_rough_count",
                "Total (rough) amount of documents",
                listOf("type")
            ).apply {
                addMetric(listOf("PASTE"), XdDocument.filter { it.type eq XdDocumentType.PASTE }.roughSize().toDouble())
                addMetric(listOf("URL"), XdDocument.filter { it.type eq XdDocumentType.URL }.roughSize().toDouble())
            },
            CounterMetricFamily(
                "dogbin_db_users_rough_count",
                "Total (rough) amount of users",
                listOf("role")
            ).apply {
                addMetric(listOf("SYSTEM"), XdUser.filter { it.role eq XdUserRole.SYSTEM }.roughSize().toDouble())
                addMetric(listOf("ANON"), XdUser.filter { it.role eq XdUserRole.ANON }.roughSize().toDouble())
                addMetric(listOf("ADMIN"), XdUser.filter { it.role eq XdUserRole.ADMIN }.roughSize().toDouble())
                addMetric(listOf("MOD"), XdUser.filter { it.role eq XdUserRole.MOD }.roughSize().toDouble())
                addMetric(listOf("USER"), XdUser.filter { it.role eq XdUserRole.USER }.roughSize().toDouble())
            },
            CounterMetricFamily(
                "dogbin_db_sessions_rough_count",
                "Total (rough) amount of sessions",
                XdSession.all().roughSize().toDouble()
            )
        )
    }

}