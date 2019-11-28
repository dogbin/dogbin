package dog.del.app.metrics

import io.prometheus.client.Collector
import io.prometheus.client.CounterMetricFamily
import io.prometheus.client.GaugeMetricFamily
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.entitystore.PersistentEntityStoreStatistics
import jetbrains.exodus.env.EnvironmentStatistics
import org.koin.core.KoinComponent
import org.koin.core.inject

class XodusCollector : Collector(), KoinComponent {
    private val store by inject<TransientEntityStore>()

    override fun collect(): MutableList<MetricFamilySamples> = mutableListOf(
        GaugeMetricFamily(
            "xodus_store_usable_space",
            "The number of available bytes on the partition where the database is located",
            store.persistentStore.usableSpace.toDouble()
        ),
        GaugeMetricFamily(
            "xodus_store_blobs_disk_usage",
            "Blobs disk usage",
            store.persistentStore.statistics.getStatisticsItem(PersistentEntityStoreStatistics.Type.BLOBS_DISK_USAGE).total.toDouble()
        ),
        GaugeMetricFamily(
            "xodus_store_caching_jobs",
            "Running caching jobs",
            store.persistentStore.statistics.getStatisticsItem(PersistentEntityStoreStatistics.Type.CACHING_JOBS).total.toDouble()
        ),
        GaugeMetricFamily(
            "xodus_env_active_transactions",
            "Active transactions",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.ACTIVE_TRANSACTIONS).total.toDouble()
        ),
        CounterMetricFamily(
            "xodus_env_bytes_moved_by_gc",
            "Bytes moved by GC",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.BYTES_MOVED_BY_GC).total.toDouble()
        ),
        CounterMetricFamily(
            "xodus_env_bytes_written",
            "Bytes written",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.BYTES_WRITTEN).total.toDouble()
        ),
        CounterMetricFamily(
            "xodus_env_bytes_read",
            "Bytes read",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.BYTES_READ).total.toDouble()
        ),
        GaugeMetricFamily(
            "xodus_env_disk_usage",
            "Disk usage",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.DISK_USAGE).total.toDouble()
        ),
        CounterMetricFamily(
            "xodus_env_flushed_transactions",
            "Flushed transactions",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.FLUSHED_TRANSACTIONS).total.toDouble()
        ),
        GaugeMetricFamily(
            "xodus_env_log_cache_hit_rate",
            "Log cache hit rate",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.LOG_CACHE_HIT_RATE).mean
        ),
        GaugeMetricFamily(
            "xodus_env_store_get_cache_hit_rate",
            "StoreGet cache hit rate",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.STORE_GET_CACHE_HIT_RATE).mean
        ),
        CounterMetricFamily(
            "xodus_env_readonly_transactions",
            "Read-only transactions",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.READONLY_TRANSACTIONS).total.toDouble()
        ),
        CounterMetricFamily(
            "xodus_env_transactions",
            "Transactions",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.TRANSACTIONS).total.toDouble()
        ),
        GaugeMetricFamily(
            "xodus_env_utilization_percent",
            "Utilization percent",
            store.persistentStore.environment.statistics.getStatisticsItem(EnvironmentStatistics.Type.UTILIZATION_PERCENT).total.toDouble()
        )
    )
}