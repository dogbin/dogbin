package dog.del.data.base.utils

import dog.del.commons.Date
import dog.del.commons.date
import kotlinx.dnq.XdEntity
import kotlinx.dnq.simple.*
import kotlinx.dnq.util.XdPropertyCachedProvider
import kotlin.reflect.KProperty

fun <R : XdEntity> xdRequiredDateProp(
    unique: Boolean = false,
    dbName: String? = null,
    default: ((R, KProperty<*>) -> Date)? = null,
    constraints: Constraints<R, Date?>? = null
) = XdPropertyCachedProvider {
    XdProperty<R, Long>(
        Long::class.java,
        dbName,
        constraints.collect().wrap<R, Long, Date> { date(it) },
        when {
            unique -> XdPropertyRequirement.UNIQUE
            default != null -> XdPropertyRequirement.OPTIONAL
            else -> XdPropertyRequirement.REQUIRED
        },
        { e, p -> default?.invoke(e, p)?.timeInMillis ?: 0L }
    ).wrap({ date(it) }, { it.timeInMillis })
}

fun <R : XdEntity> xdDateProp(
    unique: Boolean = false,
    dbName: String? = null,
    default: ((R, KProperty<*>) -> Date)? = null,
    constraints: Constraints<R, Date?>? = null
) = XdPropertyCachedProvider {
    XdProperty<R, Long>(
        Long::class.java,
        dbName,
        constraints.collect().wrap<R, Long, Date> { date(it) },
        if (unique) XdPropertyRequirement.UNIQUE else XdPropertyRequirement.OPTIONAL,
        { e, p -> default?.invoke(e, p)?.timeInMillis ?: 0L }
    ).wrap({ date(it) }, { it.timeInMillis })
}