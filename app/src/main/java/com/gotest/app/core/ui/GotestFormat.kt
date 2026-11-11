package com.gotest.app.core.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** Locale-only formatting shared by every feature. Words ("mnt", "dtk") live in strings.xml. */
object GotestFormat {
    private val indonesian: Locale = Locale.forLanguageTag("id-ID")
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesian)
    private val mediumDate = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", indonesian)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", indonesian)
    private val time = DateTimeFormatter.ofPattern("HH:mm", indonesian)

    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3_600

    /** "21 Oktober 2026". */
    fun longDate(date: LocalDate): String = longDate.format(date)

    /** "12 Okt 2025". */
    fun mediumDate(date: LocalDate): String = mediumDate.format(date)

    /** "21 Okt". */
    fun shortDate(date: LocalDate): String = shortDate.format(date)

    /** "Oktober 2026", for month section headers. */
    fun monthYear(date: LocalDate): String = monthYear.format(date)

    /** "19:00" from a time or date-time. */
    fun time(value: TemporalAccessor): String = time.format(value)

    /** "12.512". */
    fun number(value: Number): String = NumberFormat.getIntegerInstance(indonesian).format(value)

    /** "Rp110.000". */
    fun rupiah(amount: Long): String = "Rp${number(amount)}"

    /** "40:32", or "2:23:41" past an hour. */
    fun clock(totalSeconds: Int): String {
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            "%d:%02d:%02d".format(indonesian, hours, minutes, seconds)
        } else {
            "%02d:%02d".format(indonesian, minutes, seconds)
        }
    }
}
