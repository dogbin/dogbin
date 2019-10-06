package dog.del.data.migration.model

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.Binary
import org.litote.kmongo.Data
import org.litote.kmongo.Id

@Data()
data class User(
    val _id: Id<User>,
    val username: String,
    val password: Binary,
    val is_active: Boolean,
    val is_anonymous: Boolean,
    val is_system: Boolean,
    val is_authenticated: Boolean,
    val roles: List<String>
)