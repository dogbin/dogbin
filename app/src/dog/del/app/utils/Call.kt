package dog.del.app.utils

import io.ktor.application.ApplicationCall

val ApplicationCall.slug get() = parameters["slug"]!!.substringBeforeLast('.')
val ApplicationCall.hlLang get() = parameters["slug"]!!.substringAfterLast('.', "")