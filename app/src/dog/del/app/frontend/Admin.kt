package dog.del.app.frontend

import dog.del.app.session.session
import dog.del.app.session.user
import dog.del.data.base.model.config.Config
import dog.del.data.base.utils.freeze
import dog.del.data.base.utils.updateFrom
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.pebble.respondTemplate
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.toMap
import jetbrains.exodus.database.TransientEntityStore
import org.koin.ktor.ext.inject

fun Route.admin() = route("/a") {
    val db by inject<TransientEntityStore>()

    intercept(ApplicationCallPipeline.Call) {
        val isAdmin = call.session() != null && db.transactional { call.user(db).role.isAdmin }
        if (!isAdmin) {
            call.respond(HttpStatusCode.Unauthorized, "Nice try.")
            finish()
        }
    }

    route("config") {
        get {
            val config = Config.getConfig(db).freeze(db).map {
                val type = when (it.value) {
                    is String -> "textarea"
                    is Int -> "number"
                    is Boolean -> "checkbox"
                    else -> "textarea"
                }
                val value = when (it.value) {
                    is String -> it.value.toString()
                    is Int -> it.value.toString()
                    is Boolean -> if (it.value.toString() == "true") "checked" else ""
                    else -> it.value.toString()
                }
                ConfigItem(
                    it.key,
                    value,
                    type
                )
            }
            call.respondTemplate(
                "config", mapOf(
                    "title" to "Admin - Config",
                    "config" to config
                )
            )
        }
        post {
            val config = call.receiveParameters().toMap().map { parseTypeNotation(it.key, it.value.first()) }.toMap()
            Config.getConfig(db).updateFrom(db, config)
            call.respondRedirect("/a/config")
        }
    }
}

private fun parseTypeNotation(key: String, value: String): Pair<String, Any> {
    println(key)
    val parts = key.split('_', limit = 2)
    val type = parts[0]
    val actualKey = parts[1]
    return actualKey to when (type) {
        "textarea" -> value
        "number" -> value.toInt()
        "checkbox" -> value.toBoolean()
        else -> value
    }
}

data class ConfigItem(
    val key: String,
    val value: String,
    val type: String
)