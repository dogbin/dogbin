package dog.del.app.frontend

import io.ktor.routing.Routing
import io.ktor.routing.route

fun Routing.frontend() = route("/") {
    index()
    user()
}