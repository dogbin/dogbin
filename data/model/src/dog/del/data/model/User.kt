package dog.del.data.model

interface User<R : UserRole> {
    var username: String

    var password: String?

    var role: R

    fun checkPassword(password: String): Boolean
}

interface UserRole {
    var requiresPassword: Boolean
    var canSignIn: Boolean
    var isMod: Boolean
    var isAdmin: Boolean
}