package dog.del.app.utils

import com.mitchellbosecke.pebble.extension.AbstractExtension
import com.mitchellbosecke.pebble.extension.Function
import dog.del.app.config.AppConfig
import dog.del.app.stats.StatisticsReporter
import dog.del.commons.Date
import dog.del.commons.year
import dog.del.data.base.DB
import dog.del.data.base.Database
import dog.del.data.base.model.config.Config
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.inject

class DogbinPebbleExtension : AbstractExtension(), KoinComponent {
    private val store by inject<TransientEntityStore>()
    private val config by inject<Config>()
    private val reporter by inject<StatisticsReporter>()
    private val appConfig by inject<AppConfig>()

    override fun getGlobalVariables(): MutableMap<String, Any> = mutableMapOf(
        "config" to runBlocking(Dispatchers.DB) { config.freezeCached(store) },
        "year" to Date.getInstance().year,
        "stats_embed" to reporter.embedCode,
        "appConfig" to appConfig
    )

    override fun getFunctions(): MutableMap<String, Function> = mutableMapOf(
        "ghostbuster" to GhostBuster()
    )
}