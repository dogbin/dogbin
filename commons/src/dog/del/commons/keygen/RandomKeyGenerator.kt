package dog.del.commons.keygen

import java.security.SecureRandom
import kotlin.random.asKotlinRandom

class RandomKeyGenerator : KeyGenerator {
    private val random = SecureRandom().asKotlinRandom()

    override fun createKey(length: Int): String {
        val tmp = mutableListOf<Char>()
        repeat(length) {
            tmp += keyspace.random(random)
        }
        return tmp.shuffled(random).joinToString("")
    }

    companion object {
        private const val keyspace = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}