package dog.del.app.config

import io.ktor.config.ApplicationConfig
import io.ktor.util.hex
import java.io.File


class AppConfig(config: ApplicationConfig) {
    val db = DbConfig(
        location = File(config.property("dogbin.db.location").getString()),
        environment = config.property("dogbin.db.environment").getString()
    )

    val keys = Keys(
        session = hex(config.property("dogbin.keys.session").getString())
    )

    data class DbConfig(val location: File, val environment: String)
    data class Keys(val session: ByteArray)
}