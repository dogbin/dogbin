package dog.del.app.dto

import dog.del.data.model.User
import dog.del.data.model.UserRole

data class UserDto(
    val username: String,
    val role: UserRole
) {
    val displayName = role.usernameOverride ?: username
    companion object {
        fun fromUser(user: User<*>) = UserDto(
            user.username,
            user.role
        )
    }
}