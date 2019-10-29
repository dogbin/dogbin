package dog.del.commons

import kotlin.math.pow
import kotlin.math.round

fun Double.roundToDecimals(numDecimalPlaces: Int): Double {
    val factor = 10.0.pow(numDecimalPlaces.toDouble())
    return round(this * factor) / factor
}