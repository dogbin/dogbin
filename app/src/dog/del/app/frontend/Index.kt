package dog.del.app.frontend

import dog.del.commons.Date
import dog.del.commons.year
import dog.del.data.base.Database
import io.ktor.application.call
import io.ktor.pebble.respondTemplate
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import org.koin.ktor.ext.inject
import java.util.*

fun Route.index() = route("/") {
    get {
        call.respondTemplate("index", mapOf(
            "title" to "dogbin",
            "description" to "The sexiest pastebin and url-shortener ever",
            "year" to Date.getInstance().year
        ))
    }
}