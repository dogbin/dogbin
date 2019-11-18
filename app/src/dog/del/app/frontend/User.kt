package dog.del.app.frontend

import dog.del.app.config.AppConfig
import dog.del.app.dto.ApiCredentialDto
import dog.del.app.dto.FrontendDocumentDto
import dog.del.app.dto.NewApiCredentialDto
import dog.del.app.dto.UserDto
import dog.del.app.session.*
import dog.del.app.stats.StatisticsReporter
import dog.del.app.utils.hlLang
import dog.del.app.utils.locale
import dog.del.commons.keygen.RandomKeyGenerator
import dog.del.data.base.model.api.XdApiCredential
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.model.user.XdUserRole
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.pebble.respondTemplate
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.getOrFail
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.dnq.query.*
import kotlinx.dnq.util.findById
import org.koin.ktor.ext.inject
import java.text.SimpleDateFormat

fun Route.user() = route("/") {
    val store by inject<TransientEntityStore>()
    val reporter by inject<StatisticsReporter>()
    val appConfig by inject<AppConfig>()

    route("/login") {
        post {
            // This will either get the existing (anonymous) user or create a new anon user and add it to the session
            if (call.session() != null) {
                val existingUser = call.user(store)
                val isAnon = store.transactional(readonly = true) {
                    existingUser.role == XdUserRole.ANON
                }
                if (!isAnon) {
                    call.respondRedirect("/me", false)
                    return@post
                }
            }
            val params = call.receiveParameters()
            val username = params.getOrFail("username")
            val password = params.getOrFail("password")
            store.transactional(readonly = true) {
                val usr = XdUser.find(username)
                if (usr != null) {
                    if (usr.checkPassword(password)) {
                        call.setWebSession(WebSession(usr.xdId))
                        runBlocking {
                            call.respondRedirect("/me")
                        }
                        return@transactional
                    }
                }
                runBlocking {
                    call.respond(HttpStatusCode.Unauthorized, "Failed to sign in")
                }
            }
        }
        get {
            call.respondTemplate(
                "user/login", mapOf(
                    "title" to "Login to dogbin",
                    "description" to "Login to dogbin",
                    "formTitle" to "Login",
                    "secondaryLink" to "/register",
                    "secondaryTitle" to "Register"
                )
            )
        }
    }

    route("/logout") {
        get {
            call.clearWebSession()
            call.respondRedirect("/", false)
        }
    }

    route("/register") {
        post {
            // This will either get the existing (anonymous) user or create a new anon user and add it to the session
            val existingUser = call.user(store)
            val isAnon = store.transactional(readonly = true) {
                existingUser.role == XdUserRole.ANON
            }
            if (!isAnon) {
                call.respondRedirect("/me", false)
                return@post
            }
            val params = call.receiveParameters()
            val username = params.getOrFail("username")
            val password = params.getOrFail("password")
            store.transactional {
                val usr = XdUser.find(username)
                if (usr != null) {
                    runBlocking {
                        call.respond(HttpStatusCode.Conflict, "This username is taken")
                    }
                } else {
                    existingUser.signUp(username, password)
                    runBlocking {
                        call.respondRedirect("/me", false)
                    }
                    GlobalScope.launch {
                        reporter.reportEvent(StatisticsReporter.Event.USER_REGISTER, call.request)
                    }
                }
            }
        }
        get {
            call.respondTemplate(
                "user/login", mapOf(
                    "title" to "Register for dogbin",
                    "description" to "Register for dogbin",
                    "formTitle" to "Register",
                    "secondaryLink" to "/login",
                    "secondaryTitle" to "Login"
                )
            )
        }
    }

    route("/me") {
        get {
            val usr = call.user(store)
            val isAnon = store.transactional(readonly = true) {
                usr.role == XdUserRole.ANON
            }
            if (isAnon) {
                call.respondRedirect("/login", false)
                return@get
            }
            val docs = store.transactional(readonly = true) {
                XdDocument.filter { it.owner eq usr }.sortedBy(XdDocument::created, asc = false).toList()
            }.map { FrontendDocumentDto().applyFrom(it, call) }

            val user = store.transactional { UserDto.fromUser(usr, call.locale) }
            call.respondTemplate(
                "user/user", mapOf(
                    "title" to "Me",
                    "description" to "View your profile",
                    "user" to user,
                    "pastes" to docs
                )
            )
        }

        route("changepass") {
            get {
                val usr = call.user(store)
                val requiresPassword = store.transactional(readonly = true) {
                    usr.role.requiresPassword
                }
                if (!requiresPassword) {
                    call.respondRedirect("/login", false)
                    return@get
                }
                call.respondTemplate(
                    "user/changepass", mapOf(
                        "title" to "Change password",
                        "description" to "Change your dogbin password"
                    )
                )
            }

            post {
                val usr = call.user(store)
                val requiresPassword = store.transactional(readonly = true) {
                    usr.role.requiresPassword
                }
                if (!requiresPassword) {
                    call.respondRedirect("/", false)
                    return@post
                }
                val params = call.receiveParameters()
                val current = params.getOrFail("current")
                val new = params.getOrFail("password")
                store.transactional {
                    if (usr.checkPassword(current)) {
                        usr.changePass(new)
                        runBlocking {
                            call.respondRedirect("/me")
                        }
                        return@transactional
                    }
                    runBlocking {
                        call.respondRedirect("/me/changepass")
                    }
                }
            }
        }
        // TODO: this is currently pretty messy, just like most of our frontend code. We NEED to clean this up.
        route("api") {
            get {
                val usr = call.user(store)
                val requiresPassword = store.transactional(readonly = true) {
                    usr.role.requiresPassword
                }
                if (!requiresPassword) {
                    call.respondRedirect("/login", false)
                    return@get
                }
                call.respondTemplate(
                    "user/api", mapOf(
                        "title" to "API Credentials",
                        "description" to "Manage your API credentials",
                        "credentials" to store.transactional(readonly = true) {
                            XdApiCredential.findForUser(usr).map { ApiCredentialDto.fromApiCredential(it, call.locale) }
                        }
                    )
                )
            }

            route("new") {
                get {
                    val usr = call.user(store)
                    val requiresPassword = store.transactional(readonly = true) {
                        usr.role.requiresPassword
                    }
                    if (!requiresPassword) {
                        call.respondRedirect("/login", false)
                        return@get
                    }
                    call.respondTemplate(
                        "user/api_new", mapOf(
                            "title" to "New API key",
                            "description" to "Create a new API key"
                        )
                    )
                }
                post {
                    val usr = call.user(store)
                    val requiresPassword = store.transactional(readonly = true) {
                        usr.role.requiresPassword
                    }
                    if (!requiresPassword) {
                        call.respondRedirect("/", false)
                        return@post
                    }
                    val params = call.receiveParameters()
                    val name = params.getOrFail("name")
                    val canCreateDocuments = params.get("canCreateDocuments") == "on"
                    val canUpdateDocuments = params.get("canUpdateDocuments") == "on"
                    val canDeleteDocuments = params.get("canDeleteDocuments") == "on"
                    val key = RandomKeyGenerator().createKey(appConfig.api.keyLength)

                    // Actually create the credentials in the db
                    store.transactional {
                        XdApiCredential.new(key, usr)?.apply {
                            this.name = name
                            this.canCreateDocuments = canCreateDocuments
                            this.canUpdateDocuments = canUpdateDocuments
                            this.canDeleteDocuments = canDeleteDocuments
                        }
                    }
                    call.respondTemplate(
                        "user/api_created", mapOf(
                            "title" to "New API Key",
                            "description" to "Create a new API key",
                            "cred" to NewApiCredentialDto(name, key)
                        )
                    )
                }
            }

            route("delete/{id}") {
                get {
                    val usr = call.user(store)
                    val requiresPassword = store.transactional(readonly = true) {
                        usr.role.requiresPassword
                    }
                    if (!requiresPassword) {
                        call.respondRedirect("/", false)
                        return@get
                    }
                    val id = call.parameters["id"]!!
                    store.transactional {
                        XdApiCredential.findById(id).apply {
                            if (usr != user) {
                                runBlocking {
                                    call.respondRedirect("/", false)
                                }
                            } else {
                                delete()
                                runBlocking {
                                    call.respondRedirect("/me/api", false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}