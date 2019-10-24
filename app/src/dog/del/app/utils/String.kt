package dog.del.app.utils

fun String?.emptyAsNull() = if (isNullOrBlank()) null else this