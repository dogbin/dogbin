package dog.del.app.api.v1.docs

import io.ktor.routing.Route
import io.ktor.routing.route
import jetbrains.exodus.database.TransientEntityStore
import org.koin.ktor.ext.inject

fun Route.docs() = route("docs") {
    val store by inject<TransientEntityStore>()

    list(store)
    editable(store)
}
