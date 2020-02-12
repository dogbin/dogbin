package dog.del.app.session

import dog.del.data.base.DB
import dog.del.data.base.Database
import dog.del.data.base.model.user.XdUser
import io.ktor.application.ApplicationCall
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.dnq.util.findById

fun ApplicationCall.getWebSession() = sessions.get<WebSession>()
fun ApplicationCall.setWebSession(session: WebSession) {
    sessions.set(session)
}

fun ApplicationCall.clearWebSession() {
    sessions.clear<WebSession>()
}

fun ApplicationCall.session() = getWebSession()
suspend fun ApplicationCall.user(db: TransientEntityStore, isApi: Boolean = false): XdUser =
    withContext(Dispatchers.DB) {
        val session = session()
        if (session != null) {
            try {
                return@withContext db.transactional(readonly = true) {
                    XdUser.findById(session.user)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sessions.clear<WebSession>()
            }
        }

        return@withContext db.transactional {
            if (isApi) {
                XdUser.apiAnon
            } else {
                val newUser = XdUser.newAnon()
                setWebSession(WebSession(newUser.xdId))
                return@transactional newUser
            }
        }
    }