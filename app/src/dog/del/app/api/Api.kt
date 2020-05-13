package dog.del.app.api

import dog.del.app.api.v1.auth.auth
import dog.del.app.api.v1.docs.docs
import io.ktor.routing.Routing
import io.ktor.routing.route

data class ErrorDto(
    val message: String
)

fun Routing.api() = route("/api") {
    route("/v1") {
        auth()
        docs()
    }
}
