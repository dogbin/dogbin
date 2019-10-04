package dog.del.commons.keygen

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

class PhoneticKeyGenerator : KeyGenerator {
    private val random = Random(System.nanoTime())
    private val lowerChance get() = random.nextDouble() < 0.17
    private val lowChance get() = random.nextDouble() < 0.19
    private val fiftyFifty get() = random.nextDouble() < 0.5

    override fun createKey(length: Int): String {
        var lastWas = random.nextInt(0, 2)
        return (0..length).joinToString("") {
            if (lastWas == 0) {
                if (fiftyFifty) {
                    if (fiftyFifty) {
                        lastWas = CONSONANT
                        if (it == 0) {
                            commonConConSt.random(random)
                        } else {
                            commonConCon.random(random)
                        }
                    } else if (lowerChance) {
                        lastWas = CONSONANT
                        uncommonCon.random(random)
                    } else {
                        lastWas = VOWEL
                        if (it == 0) {
                            commonConVowSt.random(random)
                        } else {
                            commonConVow.random(random)
                        }
                    }
                } else {
                    lastWas = CONSONANT
                    consonants.random(random)
                }
            } else if (lowChance) {
                if (lowerChance || it == 0 && fiftyFifty) {
                    if (it == 0) {
                        if (fiftyFifty) {
                            lastWas = VOWEL
                            commonVowVowSt.random(random)
                        } else {
                            lastWas = CONSONANT
                            commonVowConSt.random(random)
                        }
                    } else {
                    lastWas = VOWEL
                        commonVowVow.random(random)
                    }
                } else {
                    lastWas = CONSONANT
                    commonVowCon.random(random)
                }
            } else {
                lastWas = VOWEL
                vowels.random(random)
            }
        }.take(length)
    }

    companion object {
        private val vowels = listOf("a", "e", "i", "o", "u", "y")
        private val consonants = listOf("b", "c", "d", "f", "g", "h", "l", "m", "n", "p", "r", "s", "t", "v", "w")
        private val uncommonCon = listOf("x", "z", "q", "j", "k")
        private val commonVowVowSt = listOf("ea", "ai", "a", "yu")
        private val commonVowVow = listOf("ee", "oo", "ea", "ai", "ay", "uy")
        private val commonVowCon = listOf("in", "an", "ing", "im", "er", "ex", "un", "est", "ux", "am", "ap")
        private val commonVowConSt = listOf("un", "im", "in", "ex")
        private val commonConVow = listOf("me", "li", "le", "ly", "pe", "re", "fi", "nu", "co", "lo", "cu", "ki", "cy", "fu", "mo", "bi")
        private val commonConVowSt = listOf("me", "li", "fu", "pe", "lo", "mo")
        private val commonConConSt = listOf("gh", "th", "gr", "st", "ph", "pr", "t", "cr")
        private val commonConCon = listOf("ll", "pp", "gh", "th", "gr", "ng", "st", "ph", "rr", "gn", "ck", "rf", "tt", "cr")

        private const val CONSONANT = 1
        private const val VOWEL = 0
    }
}