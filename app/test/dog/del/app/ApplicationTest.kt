package dog.del.app

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun `Application is running`() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/healthz").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue { response.content.parseJson()["running"].asBoolean }
            }
        }
    }

    private fun String?.parseJson() = Gson().fromJson<JsonObject>(this, JsonObject::class.java)
}
