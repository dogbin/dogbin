package dog.del.commons


// Based on the @imme_emosol regex from here: https://mathiasbynens.be/demo/url-regex
private val urlRegex = Regex("^(https?|ftp)://(-\\.)?([^\\s/?.#]+\\.?)+(/[^\\s]*)?$", RegexOption.IGNORE_CASE)
fun String.isUrl(): Boolean = trim().matches(urlRegex)