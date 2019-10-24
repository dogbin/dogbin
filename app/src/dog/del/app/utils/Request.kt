package dog.del.app.utils

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.host
import io.ktor.request.userAgent
import io.ktor.util.filter
import io.ktor.util.toMap

private const val botRegexPattern = "(bot|spider|crawl)"
private val botRegex = botRegexPattern.toRegex(setOf(RegexOption.IGNORE_CASE))

val ApplicationRequest.dnt get() = header("dnt")?.toBoolean() == true
val ApplicationRequest.isBot get() = userAgent()?.contains(botRegex) == true
val ApplicationRequest.refs
    get() = queryParameters.filter { key, _ ->
        key.toLowerCase() in listOf("utm_source", "source", "ref", "from")
    }.toMap().values.firstOrNull()?.joinToString()
val ApplicationRequest.referer get() = header("referer")
val ApplicationRequest.clientHash get() = origin.remoteHost.hashCode().toString()