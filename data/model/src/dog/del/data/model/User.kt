package dog.del.data.model

import dog.del.commons.Date

interface User<R : UserRole> {
    var username: String

    var password: String?

    var role: R

    val created: Date

    fun checkPassword(password: String): Boolean
}

interface UserRole {
    var requiresPassword: Boolean
    var canSignIn: Boolean
    var isMod: Boolean
    var isAdmin: Boolean
    var usernameOverride: String?
}