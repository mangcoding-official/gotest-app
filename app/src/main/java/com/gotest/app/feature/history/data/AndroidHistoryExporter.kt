package com.gotest.app.feature.history.data

import android.content.Context
import android.content.res.Resources
import androidx.core.net.toUri
import com.gotest.app.R
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.history.domain.ExportFormat
import com.gotest.app.feature.history.domain.ExportPeriod
import com.gotest.app.feature.history.domain.HistoryExport
import com.gotest.app.feature.history.domain.HistoryExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.io.Writer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3_600

/** Byte order mark, so Excel opens the UTF-8 file with the right encoding. */
private const val UTF8_BOM = '\uFEFF'
private const val CSV_LINE_END = "\r\n"

/** Characters that force a CSV field into quotes. */
private val csvSpecialChars = setOf(',', '"', '\n', '\r')

/**
 * Writes the export through the Storage Access Framework, so no storage permission is needed.
 * The PDF and CSV are generated on the IO dispatcher straight into the picked document.
 */
@Singleton
class AndroidHistoryExporter
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : HistoryExporter {
        override suspend fun export(
            destination: String,
            export: HistoryExport,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatching {
                    val output =
                        context.contentResolver.openOutputStream(destination.toUri())
                            ?: throw IOException("Cannot open $destination")
                    output.use { stream ->
                        when (export.format) {
                            ExportFormat.Pdf -> HistoryPdfWriter(context.resources).write(export, stream)
                            ExportFormat.Csv -> writeCsv(context.resources, export, stream)
                        }
                    }
                }
            }
    }

/** One header row, then one row per attempt: ISO date, package, type, score, accuracy and h:mm:ss. */
private fun writeCsv(
    resources: Resources,
    export: HistoryExport,
    output: OutputStream,
) {
    val writer = output.bufferedWriter(Charsets.UTF_8)
    writer.append(UTF8_BOM)
    writer.appendCsvRow(
        listOf(
            resources.getString(R.string.history_export_column_date),
            resources.getString(R.string.history_export_column_package),
            resources.getString(R.string.history_export_column_type),
            resources.getString(R.string.history_export_column_score),
            resources.getString(R.string.history_export_column_accuracy_percent),
            resources.getString(R.string.history_export_column_duration),
        ),
    )
    export.attempts.forEach { attempt ->
        val summary = attempt.summary
        writer.appendCsvRow(
            listOf(
                summary.completedOn.toString(),
                summary.title,
                resources.typeLabel(summary.type),
                summary.score.toString(),
                attempt.accuracyPercent.toString(),
                hoursMinutesSeconds(summary.durationSeconds),
            ),
        )
    }
    writer.flush()
}

/** RFC 4180: fields with a comma, quote or line break are quoted, quotes doubled. */
private fun Writer.appendCsvRow(fields: List<String>) {
    val line =
        fields.joinToString(",") { field ->
            if (field.any { it in csvSpecialChars }) {
                "\"${field.replace("\"", "\"\"")}\""
            } else {
                field
            }
        }
    append(line).append(CSV_LINE_END)
}

/** "0:20:32": always with hours, so spreadsheets read it as a duration. */
private fun hoursMinutesSeconds(totalSeconds: Int): String =
    "%d:%02d:%02d".format(
        Locale.ROOT,
        totalSeconds / SECONDS_PER_HOUR,
        totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
        totalSeconds % SECONDS_PER_MINUTE,
    )

internal fun Resources.typeLabel(type: PackageType): String =
    getString(
        when (type) {
            PackageType.Practice -> R.string.history_type_practice
            PackageType.Tryout -> R.string.history_type_tryout
        },
    )

/** "30 hari terakhir", the period as the document header names it. */
internal fun Resources.periodLabel(period: ExportPeriod): String =
    getString(
        when (period) {
            ExportPeriod.Last7Days -> R.string.history_export_document_period_7_days
            ExportPeriod.Last30Days -> R.string.history_export_document_period_30_days
            ExportPeriod.Last3Months -> R.string.history_export_document_period_3_months
            ExportPeriod.All -> R.string.history_export_document_period_all
        },
    )
