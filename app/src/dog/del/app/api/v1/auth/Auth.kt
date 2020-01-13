package dog.del.app.api.v1.auth

import dog.del.app.config.AppConfig
import io.ktor.routing.Route
import io.ktor.routing.route
import jetbrains.exodus.database.TransientEntityStore
import org.koin.ktor.ext.inject

fun Route.auth() = route("/auth") {
    val store by inject<TransientEntityStore>()
    val appConfig by inject<AppConfig>()

    login(store, appConfig)
    logout(store)
}