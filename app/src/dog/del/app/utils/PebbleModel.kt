package dog.del.app.utils

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.pebble.PebbleContent
import io.ktor.pebble.respondTemplate
import io.ktor.response.respond
import java.util.*

interface PebbleModel {
    /**
     * Should return true if response has been sent inside
     */
    suspend fun onRespond(call: ApplicationCall): Boolean

    suspend fun toModel(): Map<String, Any>
}

/**
 * Respond with the specified [template] passing [model]
 *
 * @see PebbleContent
 */
suspend fun ApplicationCall.respondTemplate(
    status: HttpStatusCode,
    template: String,
    model: Map<String, Any>,
    locale: Locale? = null,
    etag: String? = null,
    contentType: ContentType = ContentType.Text.Html.withCharset(
        Charsets.UTF_8
    )
) = respond(status, PebbleContent(template, model, locale, etag, contentType))


suspend fun ApplicationCall.respondTemplate(
    template: String,
    model: PebbleModel,
    locale: Locale? = null,
    etag: String? = null,
    contentType: ContentType = ContentType.Text.Html.withCharset(
        Charsets.UTF_8
    )
) {
    if (!model.onRespond(this)) {
        respondTemplate(template, model.toModel(), locale, etag, contentType)
    }
}