package dog.del.app.api.v1.auth

import dog.del.app.api.ErrorDto
import dog.del.app.config.AppConfig
import dog.del.app.stats.StatisticsReporter
import dog.del.commons.keygen.RandomKeyGenerator
import dog.del.data.base.model.api.XdApiCredential
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.suspended
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.gosimple.nbvcxz.Nbvcxz

data class RegistrationDtoIn(
    val username: String,
    val password: String,
    val permissions: List<String>,
    val application: String?
)

data class RegistrationDtoOut(
    val username: String,
    val apiKey: String
)

fun Route.register(store: TransientEntityStore, appConfig: AppConfig, estimator: Nbvcxz, reporter: StatisticsReporter) =
    post("register") {
        val data = call.receive<RegistrationDtoIn>()

        val result = estimator.estimate(data.password)
        if (!result.isMinimumEntropyMet) {
            call.respond(HttpStatusCode.NotAcceptable, ErrorDto("Insecure password"))
            return@post
        }

        val dto: RegistrationDtoOut? = store.suspended {
            val alreadyRegisteredUser = XdUser.find(data.username)
            if (alreadyRegisteredUser != null) {
                return@suspended null
            }

            val registeredUser = XdUser.apiAnon.signUp(data.username, data.password)

            val key = RandomKeyGenerator().createKey(appConfig.api.keyLength)

            // Actually create the credentials in the db
            XdApiCredential.new(key, registeredUser)!!.apply {
                this.name = data.application ?: "Unnamed API client"
                this.canCreateDocuments = data.permissions.contains("create")
                this.canUpdateDocuments = data.permissions.contains("update")
                this.canDeleteDocuments = data.permissions.contains("delete")
                this.canListDocuments = data.permissions.contains("list")
            }
            return@suspended RegistrationDtoOut(registeredUser.username, key)
        }

        if (dto != null) {
            call.respond(dto)
            GlobalScope.launch {
                reporter.reportEvent(StatisticsReporter.Event.USER_REGISTER, call.request)
            }
        } else {
            call.respond(HttpStatusCode.Conflict, ErrorDto("Username Taken"))
        }
    }
