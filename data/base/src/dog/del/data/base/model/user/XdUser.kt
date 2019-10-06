package dog.del.data.base.model.user

import dog.del.commons.Date
import dog.del.commons.date
import dog.del.data.base.security.Password
import dog.del.data.base.utils.xdRequiredDateProp
import dog.del.data.model.User
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.simple.requireIf
import java.util.*

class XdUser(entity: Entity) : XdEntity(entity), User<XdUserRole> {
    companion object : XdNaturalEntityType<XdUser>() {
        fun newAnon() = new {
            username = UUID.randomUUID().toString()
            role = XdUserRole.ANON
        }

        fun findOrNewSystem(username: String) = findOrNew {
            this.username = username
            role = XdUserRole.SYSTEM
        }

        fun find(username: String) = filter { it.username eq username }.firstOrNull()

        /**
         * User used for pastes created via anonymous api
         */
        val apiAnon get() = findOrNewSystem("Anonymous")

        override fun new(init: XdUser.() -> Unit): XdUser {
            return super.new {
                created = date()
                init(this)
            }
        }
    }

    fun signUp(username: String, password: String): XdUser {
        this.username = username
        this.password = password
        role = XdUserRole.USER
        return this
    }

    fun changePass(password: String) {
        this.password = password;
        // TODO: close all sessions except for the one initiating this request
    }

    override var username by xdRequiredStringProp(unique = true, trimmed = true)
    // TODO: add support for expiring passwords
    override var password: String? = null
        /**
         * Sets the password of this user **AFTER** hashing it
         */
        set(value) {
            _password = if (value != null) Password.hash(value) else null
            // Fill backing field with some data to allow checking if pw is set but don't allow snooping
            // TODO: we should however just make the "password" field on the interface protected as soon as we switch to jdk 9
            field = if (value == null) null else "Don't even try it"
        }
    /**
     * Backing password field
     */
    private var _password by xdStringProp(dbName = "password") {
        requireIf { role.requiresPassword }
    }
    override var role by xdLink1(XdUserRole)

    override var created by xdRequiredDateProp(
        default = { _, _ -> Date.getInstance() }
    )

    override fun checkPassword(password: String) =
        this._password != null && Password.verify(password, this._password!!).verified
}