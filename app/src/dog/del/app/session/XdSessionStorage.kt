package dog.del.app.session

import dog.del.data.base.model.session.XdSession
import io.ktor.sessions.SessionStorage
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.toByteArray
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.io.reader
import kotlinx.coroutines.withContext
import kotlinx.io.core.ExperimentalIoApi
import kotlinx.io.core.IoBuffer
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

@KtorExperimentalAPI
@ExperimentalIoApi
class XdSessionStorage : SessionStorage, KoinComponent {
    private val context = Dispatchers.IO + Job()
    private val db by inject<TransientEntityStore>()

    override suspend fun invalidate(id: String) {
        withContext(context) {
            db.transactional {
                XdSession.find(id)?.delete()
            }
        }
    }

    override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R = withContext(context) {
        val stream = db.transactional {
            XdSession.find(id)?.content
        } ?: throw NoSuchElementException("Session $id not found")
        consumer(stream.toByteReadChannel())
    }

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) = withContext(context) {
        provider(reader(context, autoFlush = true) {
            val buf = channel.toByteArray()
            db.transactional {
                XdSession.findOrNew(id) {
                    content = ByteArrayInputStream(buf)
                }
            }
        }.channel)
    }

}