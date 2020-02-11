package dog.del.app

import com.github.benmanes.caffeine.cache.Caffeine
import com.mitchellbosecke.pebble.cache.CacheKey
import com.mitchellbosecke.pebble.cache.tag.CaffeineTagCache
import com.mitchellbosecke.pebble.cache.template.CaffeineTemplateCache
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import com.mitchellbosecke.pebble.template.PebbleTemplate
import dog.del.app.api.api
import dog.del.app.api.ws.websocketApis
import dog.del.app.config.AppConfig
import dog.del.app.frontend.admin
import dog.del.app.frontend.frontend
import dog.del.app.frontend.legacyApi
import dog.del.app.highlighter.Highlighter
import dog.del.app.markdown.MarkdownRenderer
import dog.del.app.markdown.iframely.Iframely
import dog.del.app.metrics.DogbinCollectors
import dog.del.app.metrics.DogbinMetrics
import dog.del.app.screenshotter.Screenshotter
import dog.del.app.session.WebSession
import dog.del.app.session.XdSessionStorage
import dog.del.app.stats.StatisticsReporter
import dog.del.app.utils.DogbinPebbleExtension
import dog.del.app.utils.PasswordEstimator
import dog.del.app.utils.asExecutorService
import dog.del.commons.isUrl
import dog.del.commons.keygen.KeyGenerator
import dog.del.commons.keygen.PhoneticKeyGenerator
import dog.del.data.base.Database
import dog.del.data.base.model.caches.XdHighlighterCache
import dog.del.data.base.model.caches.XdScreenshotCache
import dog.del.data.base.model.config.Config
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.model.user.XdUser
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.pebble.Pebble
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelinePhase
import io.ktor.websocket.WebSockets
import io.prometheus.client.Gauge
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import io.prometheus.client.hotspot.DefaultExports
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import ktor_health_check.Health
import org.koin.ktor.ext.Koin
import org.koin.ktor.ext.get
import se.zensum.ktorPrometheusFeature.PrometheusFeature
import java.io.File
import kotlin.reflect.jvm.jvmName

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val appConfig = AppConfig(environment.config)

    install(PrometheusFeature) {
        disableMetricsEndpoint()
    }
    //io.ktor.DefaultExports.initialize()

    val metrics = DogbinMetrics()
    val metricsPhase = PipelinePhase("metrics")
    insertPhaseBefore(ApplicationCallPipeline.Monitoring, metricsPhase)
    intercept(metricsPhase) {
        metrics.activeRequests.inc()
        val timer = metrics.requestDuration.startTimer()
        try {
            proceed()
        } catch (e: Exception) {
            metrics.exceptions.labels((e::class.qualifiedName ?: e::class.jvmName)).inc()
            throw e
        } finally {
            timer.setDuration()
            metrics.activeRequests.dec()
        }
    }

    install(Koin) {
        // TODO: split into multiple modules
        val appModule = org.koin.dsl.module {
            single { appConfig }
            single { Highlighter() }
            single { Screenshotter() }
            single {
                runBlocking { initDb(get(), get()) }
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
            single { MarkdownRenderer() }
            single { Iframely() }
            single { PasswordEstimator.init() }
            single { metrics }
        }
        modules(
            appModule
        )
    }

    DogbinCollectors.register()

    install(ContentNegotiation) {
        gson {
        }
    }

    install(WebSockets)

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

        tagCache(
            CaffeineTagCache(
                Caffeine.newBuilder()
                    .maximumSize(200)
                    .recordStats()
                    .build<CacheKey, Any>().also {
                        get<DogbinMetrics>().cacheMetrics.addCache("pebbleTagCache", it)
                    }
            )
        )
        templateCache(
            CaffeineTemplateCache(
                Caffeine.newBuilder()
                    .maximumSize(600)
                    .recordStats()
                    .build<Any, PebbleTemplate>().also {
                        get<DogbinMetrics>().cacheMetrics.addCache("pebbleTemplateCache", it)
                    }
            )
        )
        executorService(Dispatchers.IO.asExecutorService())
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

    install(CallLogging)

    // TODO: can we configure this per route??
    install(CORS) {
        anyHost()
        allowNonSimpleContentTypes = true
        header("X-Api-Key")
    }

    install(Sessions) {
        cookie<WebSession>("doggie_session", XdSessionStorage()) {
            transform(SessionTransportTransformerMessageAuthentication(appConfig.keys.session))
        }
    }

    install(XForwardedHeaderSupport)

    routing {
        static("static") {
            resources("static")
        }
        static {
            resource("favicon.ico", resourcePackage = "static")
        }
        legacyApi()
        frontend()
        api()
        admin()
        websocketApis()
    }
}

// Initialize system documents (/about, /changelog, etc) and run some houskeeping/migrations
private suspend fun Application.initDb(
    appConfig: AppConfig,
    highlighter: Highlighter
): TransientEntityStore = withContext(Database.context) {
    val db = Database.init(appConfig.db.location, appConfig.db.environment)
    GlobalScope.launch {
        with(db.persistentStore.environment.environmentConfig) {
            envStoreGetCacheSize = 10_000
            isLogCacheNonBlocking = true
        }
        val systemUsr = db.transactional(readonly = true) { XdUser.find("dogbin")!! }
        var files = File(appConfig.documents.docsPath).walk().asSequence()
        if (!appConfig.documents.addDocsPath.isNullOrBlank()) {
            files += File(appConfig.documents.addDocsPath).walk().asSequence()
        }
        files.forEach { file ->
            if (file.isFile) {
                val slug = file.nameWithoutExtension
                val content = file.readText().trim()
                db.transactional {
                    XdDocument.findOrNew(slug) {
                        version = -1
                    }.apply {
                        if (stringContent != content || owner != systemUsr) {
                            stringContent = content
                            owner = systemUsr
                            version++
                            val isUrl = content.isUrl()
                            type = if (isUrl) XdDocumentType.URL else XdDocumentType.PASTE
                            log.info("Updated static document \'$slug\' (v$version)")
                        }
                    }
                }
            }
        }
        db.transactional {
            it.store.deleteEntityTypeRefactoring(XdHighlighterCache.entityType)
            it.store.deleteEntityTypeRefactoring(XdScreenshotCache.entityType)
        }
    }.start()
    db
}
