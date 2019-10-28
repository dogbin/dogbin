package dog.del.commons

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

typealias Date = Calendar

fun date(): Calendar = Date.getInstance()

fun date(time: Long): Calendar = Date.getInstance().apply {
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

fun Date.format(format: String, locale: Locale? = null): String =
    SimpleDateFormat(format, locale ?: Locale.getDefault()).format(time)

fun Date.format(locale: Locale? = null): String =
    (if (locale == null) SimpleDateFormat.getDateTimeInstance() else SimpleDateFormat.getDateTimeInstance(
        SimpleDateFormat.DEFAULT,
        SimpleDateFormat.DEFAULT,
        locale
    )).format(time)

fun Date.formatShort(locale: Locale? = null): String =
    (if (locale == null) SimpleDateFormat.getDateTimeInstance(
        SimpleDateFormat.SHORT,
        SimpleDateFormat.SHORT
    ) else SimpleDateFormat.getDateTimeInstance(
        SimpleDateFormat.SHORT,
        SimpleDateFormat.SHORT,
        locale
    )).format(time)

fun Date.formatLong(locale: Locale? = null): String = (if (locale == null) SimpleDateFormat.getDateTimeInstance(
    SimpleDateFormat.LONG,
    SimpleDateFormat.DEFAULT
) else SimpleDateFormat.getDateTimeInstance(
    SimpleDateFormat.LONG,
    SimpleDateFormat.DEFAULT,
    locale
)).format(time)

fun Date.formatDateLong(locale: Locale? = null): String =
    (if (locale == null) SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG) else SimpleDateFormat.getDateInstance(
        SimpleDateFormat.LONG,
        locale
    )).format(time)

fun Date.formatDate(locale: Locale? = null): String =
    (if (locale == null) SimpleDateFormat.getDateInstance() else SimpleDateFormat.getDateInstance(
        SimpleDateFormat.DEFAULT,
        locale
    )).format(time)

fun Date.formatTime(locale: Locale? = null): String =
    (if (locale == null) SimpleDateFormat.getTimeInstance() else SimpleDateFormat.getTimeInstance(
        SimpleDateFormat.DEFAULT,
        locale
    )).format(time)