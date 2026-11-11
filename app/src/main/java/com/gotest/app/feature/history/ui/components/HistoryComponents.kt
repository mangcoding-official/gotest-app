package com.gotest.app.feature.history.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestActivityRow
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.examChipTone
import java.time.LocalDate

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3_600

/** "20 mnt 32 dtk", "12 mnt", "3 jam 15 mnt". */
@Composable
internal fun shortDurationText(totalSeconds: Int): String {
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return when {
        hours > 0 -> stringResource(R.string.history_duration_hours_minutes, hours, minutes)
        minutes > 0 && seconds > 0 -> stringResource(R.string.history_duration_minutes_seconds, minutes, seconds)
        minutes > 0 -> stringResource(R.string.history_duration_minutes, minutes)
        else -> stringResource(R.string.history_duration_seconds, seconds)
    }
}

/** "20 Menit 32 Detik", the spelled-out form used by the ranking card. */
@Composable
internal fun longDurationText(totalSeconds: Int): String {
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return when {
        hours > 0 -> {
            stringResource(R.string.history_detail_duration_hours_minutes, hours, minutes)
        }

        minutes > 0 && seconds > 0 -> {
            stringResource(R.string.history_detail_duration_minutes_seconds, minutes, seconds)
        }

        minutes > 0 -> {
            stringResource(R.string.history_detail_duration_minutes, minutes)
        }

        else -> {
            stringResource(R.string.history_detail_duration_seconds, seconds)
        }
    }
}

/** "24 Total attempt": a white tile with a large number and a caption on one line. */
@Composable
fun HistoryStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Month header ("Oktober 2026") above a white card of attempts. */
@Composable
fun HistoryMonthSection(
    monthStart: LocalDate,
    attempts: List<AttemptSummary>,
    onViewResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(
            text = GotestFormat.monthYear(monthStart),
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        GotestCard(
            contentPadding = PaddingValues(GotestTheme.spacing.none),
            verticalSpacing = GotestTheme.spacing.none,
        ) {
            attempts.forEach { attempt ->
                HistoryAttemptRow(attempt = attempt, onViewResult = { onViewResult(attempt.attemptId) })
            }
        }
    }
}

/** One finished attempt: chip, title, "Tryout • 21 Okt • 20 mnt 32 dtk" and "Lihat Hasil". */
@Composable
fun HistoryAttemptRow(
    attempt: AttemptSummary,
    onViewResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel =
        stringResource(
            when (attempt.type) {
                PackageType.Practice -> R.string.history_type_practice
                PackageType.Tryout -> R.string.history_type_tryout
            },
        )
    GotestActivityRow(
        chipText = attempt.examLabel,
        title = attempt.title,
        subtitle =
            stringResource(
                R.string.history_row_subtitle,
                typeLabel,
                GotestFormat.shortDate(attempt.completedOn),
                shortDurationText(attempt.durationSeconds),
            ),
        actionText = stringResource(R.string.history_view_result),
        onAction = onViewResult,
        modifier = modifier,
        chipTone = examChipTone(attempt.examLabel),
    )
}

@Preview(showBackground = true)
@Composable
private fun HistoryComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            HistoryStatTile(value = "24", label = "Total attempt")
            HistoryMonthSection(
                monthStart = LocalDate.of(2026, 10, 1),
                attempts =
                    listOf(
                        AttemptSummary(
                            attemptId = "a1",
                            packageId = "p1",
                            title = "Simulasi UTBK-SNBT Session 2",
                            type = PackageType.Tryout,
                            examLabel = "UTBK-SNBT",
                            completedOn = LocalDate.of(2026, 10, 21),
                            durationSeconds = 1_232,
                            score = 540,
                        ),
                        AttemptSummary(
                            attemptId = "a2",
                            packageId = "p2",
                            title = "Latihan TWK CPNS Session 1",
                            type = PackageType.Practice,
                            examLabel = "CPNS",
                            completedOn = LocalDate.of(2026, 10, 12),
                            durationSeconds = 900,
                            score = 14,
                        ),
                    ),
                onViewResult = {},
            )
        }
    }
}
