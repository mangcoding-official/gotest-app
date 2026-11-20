package com.gotest.app.feature.ruang.ui.space

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.ruang.data.parseServerTime
import com.gotest.app.feature.ruang.domain.MitraProgress
import com.gotest.app.feature.ruang.domain.ProgressAttempt
import com.gotest.app.feature.ruang.domain.ProgressSubject
import com.gotest.app.feature.ruang.ui.components.formatGrade
import com.gotest.app.feature.ruang.ui.components.serverWindow
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * One student's progress in a Mitra space (web Components/Mitra/ProgressView, DEC-089): share of correct answers
 * per finished Try Out and Latihan over time, per subtest, and the list of attempts, newest first.
 */
@Composable
internal fun MitraProgressView(
    progress: MitraProgress,
    modifier: Modifier = Modifier,
) {
    val attempts = progress.attempts
    if (attempts.isEmpty()) return
    val average =
        attempts
            .sumOf { it.percent }
            .toDouble()
            .div(attempts.size)
            .roundToInt()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xl)) {
        Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            SubTitle(stringResource(R.string.ruang_progress_percent_title))
            Caption(stringResource(R.string.ruang_progress_average, average, attempts.size))
            TrendChart(attempts)
            ChartDates(attempts)
        }
        if (progress.subjects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
                SubTitle(stringResource(R.string.ruang_progress_subjects))
                progress.subjects.forEach { SubjectRow(it) }
            }
        }
        AttemptTable(attempts)
    }
}

@Composable
private fun SubjectRow(subject: ProgressSubject) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
            Text(
                text = subject.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textPrimary,
            )
            Caption(
                stringResource(R.string.ruang_progress_subject_line, subject.percent, subject.correct, subject.total),
            )
        }
        GotestProgressBar(
            progress = subject.percent / PERCENT,
            modifier = Modifier.fillMaxWidth(),
            color = GotestTheme.colors.bgBrand,
            trackColor = GotestTheme.colors.bgSubtle,
        )
    }
}

/** A plain line of percentages, oldest on the left, with 0, 50, and 100% guides (web TrendChart). */
@Composable
private fun TrendChart(attempts: List<ProgressAttempt>) {
    val line = GotestTheme.colors.bgBrand
    val guide = GotestTheme.colors.borderSubtle
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = GotestTheme.colors.textSecondary)
    val labels = TICKS.map { stringResource(R.string.progress_percent, it) }
    val measurer = rememberTextMeasurer()
    val description = stringResource(R.string.ruang_progress_chart)
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(CHART_RATIO)
                .semantics { contentDescription = description },
    ) {
        // Each label sits on its own guide line, left of the chart (web TrendChart).
        val measured = labels.map { measurer.measure(it, labelStyle) }
        val gap = LABEL_GAP.dp.toPx()
        val left = measured.maxOf { it.size.width } + gap
        val inset = POINT_RADIUS.dp.toPx() * 2
        val height = size.height - inset * 2
        val width = size.width - left - inset * 2

        fun y(percent: Int) = inset + height - percent / PERCENT * height

        fun x(index: Int) = left + inset + if (attempts.size == 1) width / 2 else index * width / (attempts.size - 1)

        TICKS.forEachIndexed { index, tick ->
            drawLine(
                color = guide,
                start = Offset(left, y(tick)),
                end = Offset(size.width, y(tick)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (tick == 0) null else PathEffect.dashPathEffect(floatArrayOf(DASH, DASH)),
            )
            val text = measured[index]
            drawText(text, topLeft = Offset(left - gap - text.size.width, y(tick) - text.size.height / 2f))
        }
        val path = Path()
        attempts.forEachIndexed { index, attempt ->
            if (index == 0) path.moveTo(x(index), y(attempt.percent)) else path.lineTo(x(index), y(attempt.percent))
        }
        drawPath(path, line, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        attempts.forEachIndexed { index, attempt ->
            drawCircle(line, radius = POINT_RADIUS.dp.toPx(), center = Offset(x(index), y(attempt.percent)))
        }
    }
}

@Composable
private fun ChartDates(attempts: List<ProgressAttempt>) {
    val first = attempts.first().submittedAt
    val last = attempts.last().submittedAt.takeIf { attempts.size > 1 }
    Row {
        Caption(first?.let(::dateWib).orEmpty(), modifier = Modifier.weight(1f))
        last?.let { Caption(dateWib(it), textAlign = TextAlign.End) }
    }
}

/** Riwayat pengerjaan: Selesai, Kegiatan, Benar, Nilai, newest first. */
@Composable
private fun AttemptTable(attempts: List<ProgressAttempt>) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        SubTitle(stringResource(R.string.ruang_progress_history))
        TableRow(
            done = { Caption(stringResource(R.string.ruang_progress_col_done)) },
            activity = { Caption(stringResource(R.string.ruang_progress_col_activity)) },
            correct = stringResource(R.string.ruang_progress_col_correct),
            score = stringResource(R.string.ruang_progress_col_score),
            heading = true,
        )
        attempts.asReversed().forEach { attempt ->
            HorizontalDivider(color = GotestTheme.colors.borderSubtle)
            TableRow(
                done = { Caption(attempt.submittedAt?.let(::serverWindow) ?: "-") },
                activity = {
                    Column {
                        Text(
                            text = attempt.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = GotestTheme.colors.textPrimary,
                        )
                        Caption(
                            stringResource(
                                if (attempt.kind == ProgressAttempt.KIND_PRACTICE) {
                                    R.string.ruang_progress_kind_practice
                                } else {
                                    R.string.ruang_progress_kind_tryout
                                },
                            ),
                        )
                    }
                },
                correct = stringResource(R.string.ruang_progress_fraction, attempt.correct, attempt.total),
                score = formatGrade(attempt.score),
            )
        }
    }
}

@Composable
private fun TableRow(
    done: @Composable () -> Unit,
    activity: @Composable () -> Unit,
    correct: String,
    score: String,
    heading: Boolean = false,
) {
    val style = if (heading) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val color = if (heading) GotestTheme.colors.textSecondary else GotestTheme.colors.textPrimary
    Row(
        modifier = Modifier.padding(vertical = GotestTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        Column(modifier = Modifier.weight(DONE_WEIGHT)) { done() }
        Column(modifier = Modifier.weight(ACTIVITY_WEIGHT)) { activity() }
        Text(text = correct, modifier = Modifier.weight(NUMBER_WEIGHT), style = style, color = color)
        Text(text = score, modifier = Modifier.weight(NUMBER_WEIGHT), style = style, color = color)
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
}

@Composable
private fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = GotestTheme.colors.textSecondary,
        textAlign = textAlign,
    )
}

/** "11 Oktober 2026" in WIB (the web's formatDateWib). */
private fun dateWib(iso: String): String =
    GotestFormat.longDate(parseServerTime(iso).atZone(ZoneId.of("Asia/Jakarta")).toLocalDate())

private val TICKS = listOf(0, 50, 100)
private const val PERCENT = 100f
private const val CHART_RATIO = 640f / 180f
private const val POINT_RADIUS = 4
private const val DASH = 8f
private const val LABEL_GAP = 6
private const val DONE_WEIGHT = 1.3f
private const val ACTIVITY_WEIGHT = 1.7f
private const val NUMBER_WEIGHT = 0.7f
