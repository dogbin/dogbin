package dog.del.app.utils

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.pebble.respondTemplate
import java.util.*

interface PebbleModel {
    /**
     * Should return true if response has been sent inside
     */
    suspend fun onRespond(call: ApplicationCall): Boolean
    suspend fun toModel(): Map<String, Any>
}

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