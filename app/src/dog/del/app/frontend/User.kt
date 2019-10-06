package dog.del.app.frontend

import dog.del.app.dto.FrontendDocumentDto
import dog.del.app.dto.UserDto
import dog.del.app.session.*
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.model.user.XdUserRole
import io.ktor.application.call
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
import kotlinx.coroutines.runBlocking
import kotlinx.dnq.query.asIterable
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.sortedBy
import org.koin.ktor.ext.inject

fun Route.user() = route("/") {
    val store by inject<TransientEntityStore>()

    route("/login") {
        post {
            // This will either get the existing (anonymous) user or create a new anon user and add it to the session
            if (call.session() != null) {
                val existingUser = call.user(store)
                val isAnon = store.transactional {
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
            store.transactional {
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
            call.respondTemplate("login", mapOf(
                "title" to "Login to dogbin",
                "description" to "Login to dogbin",
                "formTitle" to "Login",
                "secondaryLink" to "/register",
                "secondaryTitle" to "Register"
            ))
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
            val isAnon = store.transactional {
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
                }
            }
        }
        get {
            call.respondTemplate("login", mapOf(
                "title" to "Register for dogbin",
                "description" to "Register for dogbin",
                "formTitle" to "Register",
                "secondaryLink" to "/login",
                "secondaryTitle" to "Login"
            ))
        }
    }

    route("/me") {
        get {
            val usr = call.user(store)
            val isAnon = store.transactional {
                usr.role == XdUserRole.ANON
            }
            if (isAnon) {
                call.respondRedirect("/login", false)
                return@get
            }
            store.transactional {
                val docs = XdDocument.filter { it.owner eq usr }.sortedBy(XdDocument::created, asc = false)
                    .asIterable().map { FrontendDocumentDto.fromDocument(it) }
                val user = UserDto.fromUser(usr)
                runBlocking {
                    call.respondTemplate(
                        "user", mapOf(
                            "title" to "Me",
                            "description" to "View your profile",
                            "user" to user,
                            "pastes" to docs
                        )
                    )
                }
            }
        }

        route("changepass") {
            get {
                val usr = call.user(store)
                val requiresPassword = store.transactional {
                    usr.role.requiresPassword
                }
                if (!requiresPassword) {
                    call.respondRedirect("/login", false)
                    return@get
                }
                call.respondTemplate(
                    "changepass", mapOf(
                        "title" to "Change password",
                        "description" to "Change your dogbin password"
                    )
                )
            }

            post {
                val usr = call.user(store)
                val requiresPassword = store.transactional {
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
    }
}