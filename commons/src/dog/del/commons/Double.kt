package dog.del.commons

import kotlin.math.round

fun Double.roundToDecimals(numDecimalPlaces: Int): Double {
    val factor = Math.pow(10.0, numDecimalPlaces.toDouble())
    return round(this * factor) / factor
}