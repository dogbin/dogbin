package dog.del.app.config

import dog.del.app.utils.emptyAsNull
import io.ktor.config.ApplicationConfig
import io.ktor.util.hex
import java.io.File


@Suppress("unused")
class AppConfig(config: ApplicationConfig) {
    val appname = "dogbin"
    val host = config.propertyOrNull("dogbin.host")?.getString() ?: "localhost"

    val db = DbConfig(
        location = File(config.property("dogbin.db.location").getString()),
        environment = config.propertyOrNull("dogbin.db.environment")?.getString() ?: "prod"
    )

    val keys = Keys(
        session = hex(config.property("dogbin.keys.session").getString())
    )

    val stats = Stats(
        enabled = config.propertyOrNull("dogbin.stats.enabled")?.getString()?.toBoolean() ?: true,
        useSA = config.propertyOrNull("dogbin.stats.useSA")?.getString()?.toBoolean() == true
    )

    val api = Api(
        keyLength = 40
    )

    val documents = Documents(
        docsPath = config.propertyOrNull("dogbin.documents.docsPath")?.getString() ?: "documents/",
        addDocsPath = config.propertyOrNull("dogbin.documents.addDocsPath")?.getString().emptyAsNull()
    )

    val microservices = Microservices(
        highlighter = config.property("dogbin.microservices.highlighter").getString(),
        iframely = config.property("dogbin.microservices.iframely").getString(),
        screenshotter = config.property("dogbin.microservices.screenshotter").getString()
    )

    val highlighter = Highlighter(
        maxLines = config.propertyOrNull("dogbin.highlighter.maxLines")?.getString()?.toInt() ?: 10_000,
        maxChars = config.propertyOrNull("dogbin.highlighter.maxChars")?.getString()?.toInt() ?: 500_000
    )

    val screenshotter = Screenshotter(
        enabled = config.propertyOrNull("dogbin.screenshotter.enabled")?.getString()?.toBoolean() == true
    )

    val cdn = S3Config(
        s3Endpoint = config.propertyOrNull("dogbin.cdn.s3Endpoint")?.getString().orEmpty(),
        s3AccessKey = config.propertyOrNull("dogbin.cdn.s3AccessKey")?.getString().orEmpty(),
        s3Secret = config.propertyOrNull("dogbin.cdn.s3Secret")?.getString().orEmpty(),
        s3Secure = config.propertyOrNull("dogbin.cdn.s3Secure")?.getString()?.toBoolean() == true,
        s3Bucket = config.propertyOrNull("dogbin.cdn.s3Bucket")?.getString().orEmpty(),
        s3Region = config.propertyOrNull("dogbin.cdn.s3Region")?.getString().orEmpty(),
        s3Host = config.propertyOrNull("dogbin.cdn.s3Host")?.getString().orEmpty()
    )

    data class DbConfig(val location: File, val environment: String)
    data class Keys(val session: ByteArray)
    data class Stats(val enabled: Boolean, val useSA: Boolean)
    data class Api(val keyLength: Int)
    data class Documents(val docsPath: String, val addDocsPath: String?)
    data class Microservices(val highlighter: String, val iframely: String, val screenshotter: String)
    data class Highlighter(val maxLines: Int, val maxChars: Int)
    data class Screenshotter(val enabled: Boolean)
    data class S3Config(
        val s3Endpoint: String,
        val s3AccessKey: String,
        val s3Secret: String,
        val s3Secure: Boolean,
        val s3Bucket: String,
        val s3Region: String,
        val s3Host: String
    )
}