package dog.del.app.utils

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.pebble.respondTemplate
import io.ktor.request.acceptLanguageItems
import io.ktor.request.uri
import java.util.*

val ApplicationCall.slug get() = parameters["slug"]!!.substringBeforeLast('.')
val ApplicationCall.rawSlug get() = parameters["slug"]!!
val ApplicationCall.hlLang get() = parameters["slug"]!!.substringAfterLast('.', "")
val ApplicationCall.locale
    get() = request.acceptLanguageItems().mapNotNull {
        try {
            Locale.forLanguageTag(it.value)
        } catch (e: Exception) {
            null
        }
    }.firstOrNull()

suspend fun ApplicationCall.respondMessage(
    title: String,
    message: String,
    backUrl: String = request.uri,
    code: HttpStatusCode = HttpStatusCode.OK
) =
    respondTemplate(
        code,
        "message", mapOf(
            "title" to title,
            "lines" to message.lines(),
            "back_url" to backUrl
        )
    )