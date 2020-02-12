package dog.del.data.base

import dog.del.data.base.model.user.XdUser
import jetbrains.exodus.database.TransientEntityStore
import jetbrains.exodus.database.TransientStoreSession
import kotlinx.coroutines.*
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File
import java.util.concurrent.Executors

object Database {
    val dispatcher = Executors.newFixedThreadPool(32).asCoroutineDispatcher()

    suspend fun init(location: File, environment: String): TransientEntityStore = withContext(Dispatchers.DB) {
        XdModel.scanJavaClasspath()

        val store = StaticStoreContainer.init(
            dbFolder = location,
            environmentName = "dogbin-$environment"
        )

        initMetaData(XdModel.hierarchy, store)

        store.transactional {
            // Create "dogbin" user if it doesn't exist yet
            XdUser.findOrNewSystem("dogbin")
        }

        store
    }
}

val Dispatchers.DB get() = Database.dispatcher
suspend fun <T> TransientEntityStore.suspended(
    readonly: Boolean = false,
    block: (TransientStoreSession) -> T
) = withContext(Dispatchers.DB) { this@suspended.transactional(readonly = readonly, block = block) }

fun <T> TransientEntityStore.transactionalAsync(
    readonly: Boolean = false,
    block: (TransientStoreSession) -> T
) = CoroutineScope(Dispatchers.DB).async { this@transactionalAsync.transactional(readonly = readonly, block = block) }