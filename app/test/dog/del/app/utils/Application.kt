package dog.del.app.utils

import dog.del.app.module
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig

fun Application.testingModule() {
    (environment.config as MapApplicationConfig).apply {
        put("dogbin.db.location", createTempDir().absolutePath)
        put("dogbin.db.environment", "test")

        put("dogbin.keys.session", "deadbeef")
    }
    module(testing = true)
}