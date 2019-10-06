package dog.del.app.utils

import io.ktor.application.ApplicationCall
import io.ktor.request.acceptLanguageItems
import java.util.*

val ApplicationCall.slug get() = parameters["slug"]!!.substringBeforeLast('.')
val ApplicationCall.hlLang get() = parameters["slug"]!!.substringAfterLast('.', "")
val ApplicationCall.locale
    get() = request.acceptLanguageItems().mapNotNull {
        try {
            Locale.forLanguageTag(it.value)
        } catch (e: Exception) {
            null
        }
    }.firstOrNull()