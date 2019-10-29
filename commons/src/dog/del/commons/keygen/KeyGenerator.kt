package dog.del.commons.keygen

interface KeyGenerator {
    fun createKey(length: Int): String
    fun testRandomness() {
        val total = 10000000
        val distinct = (0 until total).map {
            createKey(10).apply {
                //println(this)
            }
        }.distinct().count()
        println("distinct: $distinct")
    }
}