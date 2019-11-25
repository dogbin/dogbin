package dog.del.app.api.ws

import com.google.gson.Gson
import io.ktor.client.features.json.defaultSerializer
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.Routing
import io.ktor.websocket.webSocket
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import me.gosimple.nbvcxz.Nbvcxz
import org.koin.ktor.ext.inject

fun Routing.passwordStrength() = webSocket("/pw") {
    val estimator by inject<Nbvcxz>()
    val gson = Gson()

    incoming.consumeAsFlow().mapNotNull { it as? Frame.Text }.collect {
        val result = estimator.estimate(it.readText())

        val dto = PasswordStrengthDto(
            result.isMinimumEntropyMet,
            result.basicScore,
            result.feedback?.warning,
            result.feedback?.suggestion
        )
        outgoing.send(Frame.Text(gson.toJson(dto)))
    }
}

data class PasswordStrengthDto(
    val accepted: Boolean,
    val score: Int,
    val warning: String? = null,
    val suggestions: List<String>? = null
)