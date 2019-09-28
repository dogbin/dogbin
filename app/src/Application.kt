package dog.del

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.pebble.Pebble
import io.ktor.pebble.respondTemplate
import ktor_health_check.Health

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    install(Health) {
        healthCheck("running") { true }
    }

    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
            suffix = ".html"
        })
    }

    routing {
        get("/") {
            call.respondTemplate("index", mapOf(
                "name" to "world!"
            ))
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

