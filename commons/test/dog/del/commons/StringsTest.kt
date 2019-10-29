package dog.del.commons

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class StringsTest {
    @Test
    fun `string to sha256`() {
        assertThat("test".sha256(), equalTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"))
    }

    @Test
    fun `count lines, has 3`() {
        val str = """
            1
            2
            3
        """.trimIndent()
        assertThat(str.lineCount, equalTo(3))
    }

    @Test
    fun `count lines, has 1`() {
        val str = "1"
        assertThat(str.lineCount, equalTo(1))
    }
}