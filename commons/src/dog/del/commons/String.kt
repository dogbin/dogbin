package dog.del.commons

import java.security.MessageDigest


val String.lineCount get() = lines().count()

private val digits = "0123456789abcdef".toCharArray()

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(toByteArray()).toHex()
}

// This was copy pasted from ktor
fun ByteArray.toHex(): String {
    val result = CharArray(size * 2)
    var resultIndex = 0
    val digits = digits

    for (index in 0 until size) {
        val b = get(index).toInt() and 0xff
        result[resultIndex++] = digits[b shr 4]
        result[resultIndex++] = digits[b and 0x0f]
    }

    return String(result)
}