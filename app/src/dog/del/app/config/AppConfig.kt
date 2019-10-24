package dog.del.app.config

import io.ktor.config.ApplicationConfig
import io.ktor.util.hex
import java.io.File


class AppConfig(config: ApplicationConfig) {
    val host = config.property("dogbin.host").getString()

    val db = DbConfig(
        location = File(config.property("dogbin.db.location").getString()),
        environment = config.property("dogbin.db.environment").getString()
    )

    val keys = Keys(
        session = hex(config.property("dogbin.keys.session").getString())
    )

    val stats = Stats(
        enabled = config.property("dogbin.stats.enabled").getString().toBoolean(),
        useSA = config.property("dogbin.stats.useSA").getString().toBoolean()
    )

    data class DbConfig(val location: File, val environment: String)
    data class Keys(val session: ByteArray)
    data class Stats(val enabled: Boolean, val useSA: Boolean)
}