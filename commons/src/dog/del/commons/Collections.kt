package dog.del.commons

fun <T> Iterable<T>.replace(value: T, replacement: T) = map { if (it == value) replacement else it }
