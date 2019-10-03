package dog.del.data.base.model.user

import dog.del.data.model.UserRole
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.enum.XdEnumEntityType
import kotlinx.dnq.xdBooleanProp
import kotlinx.dnq.xdStringProp

/**
 * Special roles which can be assigned to a user
 */
class XdUserRole(entity: Entity) : XdEnumEntity(entity), UserRole {
    companion object : XdEnumEntityType<XdUserRole>() {

        /**
         * User which created pastes before signing up, automatically changed to "USER" on signup
         */
        val ANON by enumField {
            canSignIn = true
            requiresPassword = false
            usernameOverride = "Anonymous"
        }

        /**
         * A normal user
         */
        val USER by enumField {
            canSignIn = true
            requiresPassword = true
        }

        /**
         * A System User / Bot (most notably the "dogbin" user used as author for the about page)
         */
        val SYSTEM by enumField {}

        /**
         * Moderators can delete reported content and have limited access to the dashboard
         */
        val MOD by enumField {
            canSignIn = true
            requiresPassword = true
            isMod = true
        }

        /**
         * Administrators have full edit rights on all pastes and access to the (upcoming) dashboard
         */
        val ADMIN by enumField {
            canSignIn = true
            requiresPassword = true
            isMod = true
            isAdmin = true
        }

    }

    override var canSignIn by xdBooleanProp()
    override var requiresPassword by xdBooleanProp()
    override var isMod by xdBooleanProp()
    override var isAdmin by xdBooleanProp()
    override var usernameOverride by xdStringProp()
}