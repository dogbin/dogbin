package dog.del

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun `Application is running`() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/healthz").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"running\":true}", response.content)
            }
        }
    }
}
