package dog.del.data.base

import dog.del.data.base.model.user.XdUser
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.coroutines.*
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File
import java.util.concurrent.Executors

object Database {
    val dispatcher = Executors.newFixedThreadPool(32).asCoroutineDispatcher()

    suspend fun init(location: File, environment: String): TransientEntityStore = withContext(dispatcher) {
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