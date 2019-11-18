package dog.del.app

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.googlecode.htmlcompressor.compressor.XmlCompressor
import com.mitchellbosecke.pebble.cache.tag.CaffeineTagCache
import com.mitchellbosecke.pebble.cache.template.CaffeineTemplateCache
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import dog.del.app.config.AppConfig
import dog.del.app.frontend.admin
import dog.del.app.frontend.frontend
import dog.del.app.frontend.legacyApi
import dog.del.app.highlighter.Highlighter
import dog.del.app.markdown.MarkdownRenderer
import dog.del.app.session.WebSession
import dog.del.app.session.XdSessionStorage
import dog.del.app.stats.StatisticsReporter
import dog.del.app.utils.DogbinPebbleExtension
import dog.del.app.utils.asExecutorService
import dog.del.commons.isUrl
import dog.del.commons.keygen.KeyGenerator
import dog.del.commons.keygen.PhoneticKeyGenerator
import dog.del.data.base.Database
import dog.del.data.base.model.config.Config
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.model.user.XdUser
import io.ktor.application.*
import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.http.ContentType
import io.ktor.http.ContentType.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.pebble.Pebble
import io.ktor.response.ApplicationSendPipeline
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.header
import io.ktor.util.asStream
import io.ktor.util.hex
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.io.readRemaining
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.io.writer
import ktor_health_check.Health
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import kotlin.coroutines.CoroutineContext

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val appConfig = AppConfig(environment.config)

    install(Koin) {
        val appModule = org.koin.dsl.module {
            single { appConfig }
            single {
                initDb(appConfig, Database.init(appConfig.db.location, appConfig.db.environment))
            }
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
            single { Highlighter() }
            single { MarkdownRenderer() }
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
        tagCache(CaffeineTagCache())
        templateCache(CaffeineTemplateCache(Caffeine.newBuilder().maximumSize(600).build()))
        executorService(Dispatchers.IO.asExecutorService())
    }

    // TODO: extract into a feature library for others to use
    val htmlCompressor = HtmlCompressor()
    val xmlCompressor = XmlCompressor()
    sendPipeline.intercept(ApplicationSendPipeline.ContentEncoding) {
        val content = subject as OutgoingContent

        suspend fun readText() = when (content) {
            is TextContent -> content.text
            is LocalFileContent -> content.file.readText()
            is OutgoingContent.WriteChannelContent -> {
                val chan = GlobalScope.writer {
                    content.writeTo(channel)
                }.channel
                chan.readRemaining().readText()
            }
            else -> null
        }
        if (content.contentType?.withoutParameters() == Text.Html) {
            val minimized = htmlCompressor.compress(readText() ?: return@intercept)
            proceedWith(TextContent(minimized, content.contentType!!, content.status))
        } else if (content.contentType?.toString()?.contains("xml") == true) {
            val minimized = xmlCompressor.compress(readText() ?: return@intercept)
            proceedWith(TextContent(minimized, content.contentType!!, content.status))
        }
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
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

// Initialize system documents (/about, /changelog, etc)
private fun initDb(appConfig: AppConfig, db: TransientEntityStore): TransientEntityStore {
    val usr = db.transactional(readonly = true) { XdUser.find("dogbin")!! }
    var files = File(appConfig.documents.docsPath).walk().asSequence()
    if (!appConfig.documents.addDocsPath.isNullOrBlank()) {
        files += File(appConfig.documents.addDocsPath).walk().asSequence()
    }
    files.forEach { file ->
        if (file.isFile) {
            val slug = file.nameWithoutExtension
            val content = file.readText()
            db.transactional {
                XdDocument.findOrNew(slug) {
                    version = -1
                }.apply {
                    if (stringContent != content || owner != usr) {
                        stringContent = content
                        owner = usr
                        version++
                        type = if (content.isUrl()) XdDocumentType.URL else XdDocumentType.PASTE
                    }
                }
            }
        }
    }
    return db
}
