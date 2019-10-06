package dog.del.app.dto

import dog.del.commons.formatDate
import dog.del.commons.formatDateLong
import dog.del.data.model.User
import dog.del.data.model.UserRole
import java.util.*

data class UserDto(
    val username: String,
    val role: UserRole,
    val created: String
) {
    val displayName = role.usernameOverride ?: username
    companion object {
        fun fromUser(user: User<*>, locale: Locale? = null) = UserDto(
            user.username,
            user.role,
            user.created.formatDate(locale)
        )
    }
}