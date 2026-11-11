package com.gotest.app.feature.history.domain

import java.time.LocalDate

private const val WEEK_DAYS = 7L
private const val MONTH_DAYS = 30L
private const val QUARTER_MONTHS = 3L

/** "Periode" pills of the export sheet: a window ending today, or everything. */
enum class ExportPeriod {
    Last7Days,
    Last30Days,
    Last3Months,
    All,
    ;

    /** First day the period covers when exporting on [today]; null when it has no start. */
    fun startDate(today: LocalDate): LocalDate? =
        when (this) {
            Last7Days -> today.minusDays(WEEK_DAYS - 1)
            Last30Days -> today.minusDays(MONTH_DAYS - 1)
            Last3Months -> today.minusMonths(QUARTER_MONTHS).plusDays(1)
            All -> null
        }

    /** The attempts finished within the period, in their original order. */
    fun select(
        attempts: List<HistoryAttempt>,
        today: LocalDate,
    ): List<HistoryAttempt> {
        val start = startDate(today) ?: return attempts
        return attempts.filter { !it.summary.completedOn.isBefore(start) }
    }
}

/** "Format file" options; [mimeType] and [extension] name the document the student saves. */
enum class ExportFormat(
    val mimeType: String,
    val extension: String,
) {
    Pdf("application/pdf", "pdf"),
    Csv("text/csv", "csv"),
}

/** One "Unduh File": [attempts] are newest first and all fall within [period]. */
data class HistoryExport(
    val format: ExportFormat,
    val period: ExportPeriod,
    val attempts: List<HistoryAttempt>,
    val exportedOn: LocalDate,
) {
    /** "gotest-riwayat-2026-10-30.pdf", suggested in the system save dialog. */
    val fileName: String get() = "gotest-riwayat-$exportedOn.${format.extension}"
}

/** Writes an export into a document the student picked with the system file picker. */
interface HistoryExporter {
    /** [destination] is the picked document's content URI. Fails when the file cannot be written. */
    suspend fun export(
        destination: String,
        export: HistoryExport,
    ): Result<Unit>
}
