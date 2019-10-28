package dog.del.app.dto

import dog.del.commons.formatShort
import dog.del.data.base.model.api.XdApiCredential
import java.util.*

data class ApiCredentialDto(
    val id: String,
    val name: String,
    val created: String
) {
    companion object {
        fun fromApiCredential(credential: XdApiCredential, locale: Locale? = null) = ApiCredentialDto(
            id = credential.xdId,
            name = credential.name,
            created = credential.created.formatShort(locale)
        )
    }
}

data class NewApiCredentialDto(
    val name: String,
    val key: String
)