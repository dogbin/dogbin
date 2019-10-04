package dog.del.app.session

import dog.del.commons.Date
import dog.del.commons.date

sealed class Session {
    abstract val user: String
    abstract val created: Long
}
data class WebSession(
    override val user: String,
    override val created: Long = date().timeInMillis
) : Session()
data class ApiSession(
    override val user: String,
    val name: String,
    override val created: Long = date().timeInMillis
): Session()