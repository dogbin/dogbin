package dog.del.commons

import java.util.*

typealias Date = Calendar

fun date(time: Long) = Date.getInstance().apply {
    timeInMillis = time
}

var Date.year
    get() = get(Date.YEAR)
    set(value) = set(Date.YEAR, value)

var Date.month
    get() = get(Date.MONTH)
    set(value) = set(Date.MONTH, value)

var Date.day
    get() = get(Date.DAY_OF_MONTH)
    set(value) = set(Date.DAY_OF_MONTH, value)

var Date.hour
    get() = get(Date.HOUR_OF_DAY)
    set(value) = set(Date.HOUR_OF_DAY, value)

var Date.minute
    get() = get(Date.MINUTE)
    set(value) = set(Date.MINUTE, value)

fun Date.add(
    years: Int = 0,
    months: Int = 0,
    weeks: Int = 0,
    days: Int = 0,
    hours: Int = 0,
    minutes: Int = 0,
    seconds: Int = 0
): Date = (clone() as Date).apply {
    add(Date.YEAR, years)
    add(Date.MONTH, months)
    add(Date.WEEK_OF_YEAR, weeks)
    add(Date.DATE, days)
    add(Date.HOUR, hours)
    add(Date.MINUTE, minutes)
    add(Date.SECOND, seconds)
}

fun Date.sub(
    years: Int = 0,
    months: Int = 0,
    weeks: Int = 0,
    days: Int = 0,
    hours: Int = 0,
    minutes: Int = 0,
    seconds: Int = 0
): Date = add(-years, -months, -weeks, -days, -hours, -minutes, -seconds)