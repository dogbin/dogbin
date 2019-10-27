package dog.del.app

import com.mitchellbosecke.pebble.loader.ClasspathLoader
import dog.del.app.config.AppConfig
import dog.del.app.frontend.admin
import dog.del.app.frontend.frontend
import dog.del.app.frontend.legacyApi
import dog.del.app.session.WebSession
import dog.del.app.session.XdSessionStorage
import dog.del.app.stats.StatisticsReporter
import dog.del.app.utils.DogbinPebbleExtension
import dog.del.commons.keygen.KeyGenerator
import dog.del.commons.keygen.PhoneticKeyGenerator
import dog.del.data.base.Database
import dog.del.data.base.model.config.Config
import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.JsonSerializer
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.pebble.Pebble
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.header
import io.ktor.util.hex
import jetbrains.exodus.database.TransientEntityStore
import ktor_health_check.Health
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val appConfig = AppConfig(environment.config)

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }


    install(Koin) {
        val appModule = org.koin.dsl.module {
            single { appConfig }
            single { Database.init(appConfig.db.location, appConfig.db.environment) }
            single { Config.getConfig(get()) }
            single<KeyGenerator> { PhoneticKeyGenerator() }
            single { StatisticsReporter.getReporter(appConfig) }
            single { this@module.log }
            single {
                HttpClient(Apache) {
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                }
            }
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
        extension(DogbinPebbleExtension())
    }

    install(Sessions) {
        cookie<WebSession>("doggie_session", XdSessionStorage()) {
            transform(SessionTransportTransformerMessageAuthentication(appConfig.keys.session))
        }
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
        admin()
    }
}

