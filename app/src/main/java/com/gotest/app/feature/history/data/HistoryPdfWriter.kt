package com.gotest.app.feature.history.data

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.gotest.app.R
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.history.domain.HistoryAttempt
import com.gotest.app.feature.history.domain.HistoryExport
import java.io.OutputStream

// A4 in PostScript points, the unit PdfDocument pages use.
private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

private const val TITLE_SIZE = 18f
private const val META_SIZE = 10f
private const val TABLE_SIZE = 9f
private const val TITLE_LINE = 26f
private const val META_LINE = 16f
private const val TITLE_BLOCK_GAP = 12f
private const val ROW_HEIGHT = 22f
private const val CELL_PADDING = 6f
private const val FOOTER_HEIGHT = 24f

// Column widths as fractions of the content width: date, package, type, score/accuracy, duration.
private val columnWeights = floatArrayOf(0.16f, 0.4f, 0.13f, 0.17f, 0.14f)

// Brand colours from the design tokens, as ARGB ints for android.graphics.
private const val COLOR_TITLE = 0xFF2C0942.toInt()
private const val COLOR_TEXT = 0xFF3D3D3D.toInt()
private const val COLOR_SECONDARY = 0xFF7A7A7A.toInt()
private const val COLOR_HEADER_FILL = 0xFFF3EAFD.toInt()
private const val COLOR_DIVIDER = 0xFFE7E7E7.toInt()

/**
 * A plain A4 report: title, period and count, then a table of attempts (tanggal, paket, jenis,
 * skor / akurasi, durasi) that continues on as many pages as needed, each with its header row.
 */
internal class HistoryPdfWriter(
    private val resources: Resources,
) {
    private val titlePaint = textPaint(TITLE_SIZE, COLOR_TITLE, bold = true)
    private val metaPaint = textPaint(META_SIZE, COLOR_SECONDARY, bold = false)
    private val headerPaint = textPaint(TABLE_SIZE, COLOR_TITLE, bold = true)
    private val cellPaint = textPaint(TABLE_SIZE, COLOR_TEXT, bold = false)
    private val fillPaint = Paint()

    fun write(
        export: HistoryExport,
        output: OutputStream,
    ) {
        val pages = paginate(export.attempts)
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, rows ->
                val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(info)
                val canvas = page.canvas
                var top = MARGIN
                if (index == 0) top = drawTitleBlock(canvas, export, top)
                top = drawRow(canvas, headerCells(), top, headerPaint, fill = COLOR_HEADER_FILL)
                rows.forEach { attempt -> top = drawRow(canvas, cells(attempt), top, cellPaint, fill = null) }
                drawFooter(canvas, page = index + 1, pageCount = pages.size)
                document.finishPage(page)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    /** Splits the rows so each page fits; the first page also holds the title block. */
    private fun paginate(attempts: List<HistoryAttempt>): List<List<HistoryAttempt>> {
        val tableHeight = PAGE_HEIGHT - MARGIN * 2 - FOOTER_HEIGHT - ROW_HEIGHT
        val firstPageRows = ((tableHeight - titleBlockHeight()) / ROW_HEIGHT).toInt()
        val otherPageRows = (tableHeight / ROW_HEIGHT).toInt()
        val pages = mutableListOf(attempts.take(firstPageRows))
        attempts.drop(firstPageRows).chunked(otherPageRows).forEach(pages::add)
        return pages
    }

    private fun titleBlockHeight(): Float = TITLE_LINE + META_LINE * 2 + TITLE_BLOCK_GAP

    /** "Rekap Hasil Latihan & Tryout", the period with its dates, and the count with the export date. */
    private fun drawTitleBlock(
        canvas: Canvas,
        export: HistoryExport,
        top: Float,
    ): Float {
        var baseline = top + TITLE_SIZE
        canvas.drawText(resources.getString(R.string.history_export_document_title), MARGIN, baseline, titlePaint)
        baseline += TITLE_LINE - TITLE_SIZE + META_LINE
        canvas.drawText(periodLine(export), MARGIN, baseline, metaPaint)
        baseline += META_LINE
        val count =
            resources.getString(
                R.string.history_export_document_count,
                GotestFormat.number(export.attempts.size),
                GotestFormat.mediumDate(export.exportedOn),
            )
        canvas.drawText(count, MARGIN, baseline, metaPaint)
        return top + titleBlockHeight()
    }

    private fun periodLine(export: HistoryExport): String {
        val dates = export.attempts.map { it.summary.completedOn }
        val first = dates.minOrNull()
        val last = dates.maxOrNull()
        val period = resources.periodLabel(export.period)
        return if (first == null || last == null) {
            resources.getString(R.string.history_export_document_period, period)
        } else {
            resources.getString(
                R.string.history_export_document_period_range,
                period,
                GotestFormat.mediumDate(first),
                GotestFormat.mediumDate(last),
            )
        }
    }

    private fun headerCells(): List<String> =
        listOf(
            resources.getString(R.string.history_export_column_date),
            resources.getString(R.string.history_export_column_package),
            resources.getString(R.string.history_export_column_type),
            resources.getString(R.string.history_export_column_score_accuracy),
            resources.getString(R.string.history_export_column_duration),
        )

    private fun cells(attempt: HistoryAttempt): List<String> {
        val summary = attempt.summary
        return listOf(
            GotestFormat.mediumDate(summary.completedOn),
            summary.title,
            resources.typeLabel(summary.type),
            resources.getString(
                R.string.history_export_score_accuracy,
                GotestFormat.number(summary.score),
                resources.getString(R.string.percent_value, attempt.accuracyPercent),
            ),
            GotestFormat.clock(summary.durationSeconds),
        )
    }

    /** One table row with an optional fill and a hairline below; long text is cut with "…". */
    private fun drawRow(
        canvas: Canvas,
        cells: List<String>,
        top: Float,
        paint: TextPaint,
        fill: Int?,
    ): Float {
        val bottom = top + ROW_HEIGHT
        if (fill != null) {
            fillPaint.color = fill
            canvas.drawRect(MARGIN, top, MARGIN + CONTENT_WIDTH, bottom, fillPaint)
        }
        val baseline = top + ROW_HEIGHT / 2 - (paint.descent() + paint.ascent()) / 2
        var left = MARGIN
        cells.forEachIndexed { index, text ->
            val width = CONTENT_WIDTH * columnWeights[index]
            val fitted = TextUtils.ellipsize(text, paint, width - CELL_PADDING * 2, TextUtils.TruncateAt.END)
            canvas.drawText(fitted, 0, fitted.length, left + CELL_PADDING, baseline, paint)
            left += width
        }
        fillPaint.color = COLOR_DIVIDER
        canvas.drawLine(MARGIN, bottom, MARGIN + CONTENT_WIDTH, bottom, fillPaint)
        return bottom
    }

    /** "Halaman 1 dari 2", right-aligned at the bottom margin. */
    private fun drawFooter(
        canvas: Canvas,
        page: Int,
        pageCount: Int,
    ) {
        val text = resources.getString(R.string.history_export_document_page, page, pageCount)
        val x = MARGIN + CONTENT_WIDTH - metaPaint.measureText(text)
        canvas.drawText(text, x, PAGE_HEIGHT - MARGIN, metaPaint)
    }

    private fun textPaint(
        size: Float,
        color: Int,
        bold: Boolean,
    ): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
}
