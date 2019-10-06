package dog.del.data.migration.model

import org.litote.kmongo.Data
import org.litote.kmongo.Id

@Data()
data class MongoDocument(
    val _id: String,
    val isUrl: Boolean,
    val content: String,
    val viewCount: Int,
    val version: Int,
    val owner: Id<User>
)