package dog.del.app.api.v1.auth

import dog.del.app.config.AppConfig
import dog.del.commons.keygen.RandomKeyGenerator
import dog.del.data.base.model.api.XdApiCredential
import dog.del.data.base.model.user.XdUser
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.runBlocking

data class LoginDtoIn(
    val username: String,
    val password: String,
    val permissions: List<String>,
    val application: String?
)

data class LoginDtoOut(
    val username: String,
    val apiKey: String
)

fun Route.login(store: TransientEntityStore, appConfig: AppConfig) = post("login") {
    val data = call.receive<LoginDtoIn>()
    store.transactional {
        val usr = XdUser.find(data.username)
        if (usr != null) {
            if (usr.checkPassword(data.password)) {
                val key = RandomKeyGenerator().createKey(appConfig.api.keyLength)

                // Actually create the credentials in the db
                XdApiCredential.new(key, usr)!!.apply {
                    this.name = data.application ?: "Unnamed API client"
                    this.canCreateDocuments = data.permissions.contains("create")
                    this.canUpdateDocuments = data.permissions.contains("update")
                    this.canDeleteDocuments = data.permissions.contains("delete")
                    this.canListDocuments = data.permissions.contains("list")
                }
                runBlocking {
                    call.respond(LoginDtoOut(data.username, key))
                }
                return@transactional
            }
        }
        runBlocking {
            call.respond(HttpStatusCode.Unauthorized, "Username or password incorrect")
        }
    }
}