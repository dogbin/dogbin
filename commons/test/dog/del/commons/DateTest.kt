package dog.del.commons

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class DateTest {
    companion object {
        // Saturday, August 7, 1999 9:45:00 PM UTC
        const val TEST_EPOCH = 934062300000

        @BeforeAll
        @JvmStatic
        fun `Set timezone to UTC`() {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        }
    }

    @Test
    fun `Init date with epoch, is expected date`() {
        val date = date(TEST_EPOCH)
        assertThat(date.year, equalTo(1999))
        assertThat(date.month, equalTo(7))
        assertThat(date.day, equalTo(7))
        assertThat(date.hour, equalTo(21))
        assertThat(date.minute, equalTo(45))
        assertThat(date.second, equalTo(0))
    }

    @Test
    fun `Add one of each`() {
        val date = date(TEST_EPOCH).add(
            years = 1,
            months = 1,
            days = 1,
            hours = 1,
            minutes = 1,
            seconds = 1
        )
        assertThat(date.year, equalTo(2000))
        assertThat(date.month, equalTo(8))
        assertThat(date.day, equalTo(8))
        assertThat(date.hour, equalTo(22))
        assertThat(date.minute, equalTo(46))
        assertThat(date.second, equalTo(1))
    }

    @Test
    fun `Subtract one of each`() {
        val date = date(TEST_EPOCH).sub(
            years = 1,
            months = 1,
            days = 1,
            hours = 1,
            minutes = 1,
            seconds = 1
        )
        assertThat(date.year, equalTo(1998))
        assertThat(date.month, equalTo(6))
        assertThat(date.day, equalTo(6))
        assertThat(date.hour, equalTo(20))
        assertThat(date.minute, equalTo(43))
        assertThat(date.second, equalTo(59))
    }
}