package dog.del.app.config

import dog.del.app.utils.emptyAsNull
import io.ktor.config.ApplicationConfig
import io.ktor.util.hex
import java.io.File


class AppConfig(config: ApplicationConfig) {
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

    data class DbConfig(val location: File, val environment: String)
    data class Keys(val session: ByteArray)
    data class Stats(val enabled: Boolean, val useSA: Boolean)
    data class Api(val keyLength: Int)
    data class Documents(val docsPath: String, val addDocsPath: String?)
}