package dog.del.commons

import java.security.MessageDigest


val String.lineCount get() = lines().count()

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(toByteArray()).toHex()
}

fun ByteArray.toHex() = joinToString("") {
    val hex: String = Integer.toHexString(0xff and it.toInt())
    if (hex.length == 1) "0" else hex
}