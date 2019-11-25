package dog.del.data.migration

import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import dog.del.commons.date
import dog.del.commons.isUrl
import dog.del.data.base.model.document.XdDocument
import dog.del.data.base.model.document.XdDocumentType
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.model.user.XdUserRole
import dog.del.data.migration.model.MongoDocument
import dog.del.data.migration.model.User
import jetbrains.exodus.database.TransientEntityStore
import me.tongfei.progressbar.ProgressBar
import org.litote.kmongo.*

class MongoMigration(
    val xdStore: TransientEntityStore,
    mongoHost: String = "localhost",
    dbName: String = "dogbin",
    credential: MongoCredential? = null
) {
    private val client = KMongo.createClient(ServerAddress(mongoHost), listOfNotNull(credential))
    private val database = client.getDatabase(dbName)
    private val mongoDocuments = database.getCollectionOfName<MongoDocument>("mongo_document")
    private val users = database.getCollection<User>()

    fun migrate() {
        val total = mongoDocuments.estimatedDocumentCount()
        println("Starting migration of $total documents")

        val pb = ProgressBar("Migration", total)
        mongoDocuments.find().forEach { doc ->
            val slug = doc._id.sanitize()
            pb.extraMessage = slug

            try {
                migrateDocument(doc, slug)
            } catch (e: Exception) {
                println("\nError while migrating \"$slug\": $e")
            }

            pb.step()
        }
        pb.close()
        println("Done!")
    }

    private fun migrateDocument(document: MongoDocument, slug: String) = xdStore.transactional {
        XdDocument.findOrNew(slug) {
            stringContent = document.content
            type = if (document.content.isUrl()) XdDocumentType.URL else XdDocumentType.PASTE
            version = document.version
            viewCount = document.viewCount
            owner = migrateUser(findUser(document.owner))
            created = date()
        }
    }

    private fun findUser(id: Id<User>?) =
        if (id == null) null else users.find("{'_id':ObjectId(\"$id\")}").first()

    private fun migrateUser(user: User?): XdUser {
        // Use api anon for all existing anonymous users as sessions will not be preserved anyways
        return if (user == null || user.is_anonymous) {
            XdUser.apiAnon
        } else {
            // Only migrate once and return existing user otherwise
            XdUser.findOrNew(user.username) {
                created = date()
                role = when {
                    user.is_anonymous -> XdUserRole.ANON
                    user.is_system -> XdUserRole.SYSTEM
                    "admin" in user.roles -> XdUserRole.ADMIN
                    else -> XdUserRole.USER
                }
                if (role.requiresPassword) {
                    // Ensure jhash compatibility
                    val hash = String(user.password.data).replace("$2b", "$2a")
                    setPasswordHashed("bcrypt:12:60:16:n::$hash")
                }
            }
        }
    }

    private fun String.sanitize(): String = this.substringBefore('.')
}