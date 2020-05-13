package dog.del.app.api.v1.auth

import dog.del.app.config.AppConfig
import dog.del.app.stats.StatisticsReporter
import io.ktor.routing.Route
import io.ktor.routing.route
import jetbrains.exodus.database.TransientEntityStore
import me.gosimple.nbvcxz.Nbvcxz
import org.koin.ktor.ext.inject

fun Route.auth() = route("/auth") {
    val store by inject<TransientEntityStore>()
    val appConfig by inject<AppConfig>()
    val estimator by inject<Nbvcxz>()
    val reporter by inject<StatisticsReporter>()

    login(store, appConfig)
    logout(store)
    register(store, appConfig, estimator, reporter)
}
