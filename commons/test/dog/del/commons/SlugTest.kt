package dog.del.commons

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class SlugTest {
    @TestFactory
    fun `is valid slug`() = listOf(
        "about",
        "blog",
        "bl_og",
        "-blog",
        "b-log",
        "PutoAntón",
        "LOVE七七",
        "\uD83D\uDC36\uD83D\uDDD1",
        "павло_тичина",
        "hasła",
        "ЕБАТЦАТОП",
        "虫虫虫"
    ).map { slug ->
        DynamicTest.dynamicTest("Is valid slug `$slug`") {
            Assertions.assertTrue(slug.validSlug(), "Expected `$slug` to be detected as valid slug")
        }
    }

    @TestFactory
    fun `is invalid slug`() = listOf(
        "///",
        "//a",
        "a",
        "a a",
        "     ",
        "\ntest",
        "jquery.js",
        "1+2"
    ).map { slug ->
        DynamicTest.dynamicTest("Is invalid slug `$slug`") {
            Assertions.assertFalse(slug.validSlug(), "Expected `$slug` to be detected as invalid slug")
        }
    }
}