package com.example.salaahwidget

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** One row of the daily timetable. */
data class PrayerEntry(
    val name: String,
    val time: Date,
    /** Sunrise is not a prayer; it ends the Fajr window. */
    val isPrayer: Boolean = true,
)

/**
 * Prayer times for Cape Town, South Africa.
 *
 * Calculation: Muslim World League (Fajr 18°, Isha 17°), Asr: Standard / Shafi'i.
 * Everything is computed on-device; no network is used.
 */
object PrayerTimesFactory {

    /** Cape Town local time (SAST, UTC+2, no DST). */
    val zone: ZoneId = ZoneId.of("Africa/Johannesburg")

    // Cape Town city centre: 33.9249° S, 18.4241° E.
    private val coordinates = Coordinates(-33.9249, 18.4241)

    private val parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.apply {
        madhab = Madhab.SHAFI
        // To match your masjid's timetable, add minute tweaks, e.g.:
        // adjustments.fajr = 2
    }

    /**
     * Prayer times for the calendar date of [day] in Cape Town.
     * The returned [Date]s are absolute instants; format them with [zone].
     */
    fun prayerTimesFor(day: ZonedDateTime = ZonedDateTime.now(zone)): List<PrayerEntry> {
        val dateComponents = DateComponents(day.year, day.monthValue, day.dayOfMonth)
        val times = PrayerTimes(coordinates, dateComponents, parameters)
        return listOf(
            PrayerEntry("Fajr", times.fajr),
            PrayerEntry("Sunrise", times.sunrise, isPrayer = false),
            PrayerEntry("Dhuhr", times.dhuhr),
            PrayerEntry("Asr", times.asr),
            PrayerEntry("Maghrib", times.maghrib),
            PrayerEntry("Isha", times.isha),
        )
    }

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.US).withZone(zone)

    private val shortDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.US).withZone(zone)

    private val longDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.US).withZone(zone)

    fun formatTime(time: Date): String = timeFormatter.format(time.toInstant())

    fun formatDate(day: ZonedDateTime): String = shortDateFormatter.format(day)

    fun formatLongDate(day: ZonedDateTime): String = longDateFormatter.format(day)

    /** Epoch millis of the next midnight in Cape Town (start of tomorrow). */
    fun nextMidnightMillis(): Long = ZonedDateTime.now(zone)
        .plusDays(1)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}
