package dog.del.data.base

import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.model.user.XdUser
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.size
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.store.container.createTransientEntityStore
import kotlinx.dnq.util.initMetaData
import java.io.File

object Database {

    fun init(location: File, environment: String): TransientEntityStore {
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

        return store
    }
}