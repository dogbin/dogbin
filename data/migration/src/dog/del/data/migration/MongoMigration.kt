package dog.del.data.migration

import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import dog.del.commons.date
import dog.del.commons.isUrl
import dog.del.commons.roundToDecimals
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.model.user.XdUserRole
import dog.del.data.migration.model.MongoDocument
import dog.del.data.migration.model.User
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.creator.findOrNew
import org.bson.types.ObjectId
import org.litote.kmongo.*

class MongoMigration(
    val xdStore: TransientEntityStore,
    mongoHost: String = "localhost",
    dbName: String = "dogbin_dev",
    credential: MongoCredential? = null
) {
    private val client = KMongo.createClient(ServerAddress(mongoHost), listOfNotNull(credential))
    private val database = client.getDatabase(dbName)
    private val mongoDocuments = database.getCollectionOfName<MongoDocument>("mongo_document")
    private val users = database.getCollection<User>()

    private var userCount = 0
    private var docCount = 0

    fun migrate() {
        // Reset counters
        userCount = 0
        docCount = 0

        val count = mongoDocuments.countDocuments()
        println("Starting migration of $count documents")
        mongoDocuments.find().forEachIndexed { i, it ->
            try {
                migrateDocument(it)
                print("Migrated ${it._id} successfully")
            } catch (e: Exception) {
                print("Failed to migrate ${it._id}: ${e.message}")
            }
            val percent = ((i + 1 / count.toDouble()) * 100).roundToDecimals(2)
            println(" ${i + 1}/$count ($percent%) ")
        }
        println("Done! Migrated $docCount documents and $userCount users")
    }

    private fun migrateDocument(document: MongoDocument): XdDocument = xdStore.transactional {
        XdDocument.findOrNew(document._id) {
            stringContent = document.content
            type = if (document.content.isUrl()) XdDocumentType.URL else XdDocumentType.PASTE
            version = document.version
            viewCount = document.viewCount
            owner = migrateUser(findUser(document.owner))
            created = date()
            docCount++
        }
    }

    private fun findUser(id: Id<User>) = users.find("{'_id':ObjectId(\"$id\")}").first()

    private fun migrateUser(user: User): XdUser = xdStore.transactional {
        // Use api anon for all existing anonymous users as sessions will not be preserved anyways
        if (user.is_anonymous) {
            XdUser.apiAnon
        } else {
            // Only migrate once and return existing user otherwise
            XdUser.findOrNew(user.username) {
                password = String(user.password.data)
                created = date()
                role = when {
                    user.is_anonymous -> XdUserRole.ANON
                    user.is_system -> XdUserRole.SYSTEM
                    "admin" in user.roles -> XdUserRole.ADMIN
                    else -> XdUserRole.USER
                }
                userCount++
            }
        }
    }
}