package dog.del.app.utils

import com.mitchellbosecke.pebble.extension.AbstractExtension
import dog.del.app.stats.StatisticsReporter
import dog.del.commons.Date
import dog.del.commons.year
import dog.del.data.base.model.config.Config
import jetbrains.exodus.database.TransientEntityStore
import org.koin.core.KoinComponent
import org.koin.core.inject

class DogbinPebbleExtension : AbstractExtension(), KoinComponent {
    private val store by inject<TransientEntityStore>()
    private val config by inject<Config>()
    private val reporter by inject<StatisticsReporter>()

    override fun getGlobalVariables(): MutableMap<String, Any> = mutableMapOf(
        "config" to config.freezeCached(store),
        "year" to Date.getInstance().year,
        "stats_embed" to reporter.embedCode
    )
}