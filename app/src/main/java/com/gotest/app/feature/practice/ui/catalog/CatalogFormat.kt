package com.gotest.app.feature.practice.ui.catalog

import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale.forLanguageTag("id-ID")
private val shortFormat = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)
private val longFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", indonesian)

/** "14 Nov 2026" in the phone's zone, as the web's `shortDate` does in the browser's; null without a date. */
fun shortDate(value: String?): String? =
    value
        ?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() }
        ?.let(shortFormat::format)

/** "14 November 2026" (web `formatCompletedDate`); null without a date. */
fun longDate(value: String?): String? =
    value
        ?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() }
        ?.let(longFormat::format)

/** A score as `toLocaleString('id-ID')` writes it: "75", "82,5". */
fun plainNumber(value: Double): String = NumberFormat.getNumberInstance(indonesian).format(value)
