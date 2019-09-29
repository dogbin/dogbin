package dog.del.data.base.security

import at.favre.lib.crypto.bcrypt.BCrypt

object Password {
    fun hash(password: String) = BCrypt.withDefaults().hashToString(12, password.toCharArray())
    fun verify(password: String, hash: String) = BCrypt.verifyer().verify(password.toByteArray(), hash.toByteArray())
}