package dog.del.app

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import dog.del.app.frontend.frontend
import dog.del.app.frontend.legacyApi
import dog.del.data.base.Database
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.pebble.Pebble
import io.ktor.pebble.respondTemplate
import jetbrains.exodus.database.TransientEntityStore
import ktor_health_check.Health
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import java.io.File

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

    install(Koin) {
        val appModule = org.koin.dsl.module {
            // TODO: introduce config system
            single { Database.init(File("dev.xdb"), "dev") }
        }
        modules(
            appModule
        )
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    install(Health) {
        healthCheck("running") { true }
        healthCheck("database") {
            val db = get<TransientEntityStore>()
            db.isOpen
        }
    }

    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
            suffix = ".peb"
        })
    }

    routing {
        static("static") {
            resources("static")
        }
        static {
            resource("favicon.ico", resourcePackage = "static")
        }
        legacyApi()
        frontend()
    }
}

