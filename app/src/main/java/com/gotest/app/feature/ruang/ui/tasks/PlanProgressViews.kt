package com.gotest.app.feature.ruang.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.PlanCompletion
import com.gotest.app.feature.ruang.domain.PlanProgress
import com.gotest.app.feature.ruang.domain.PlanProgressHistory
import com.gotest.app.feature.ruang.domain.PlanProgressPlan
import com.gotest.app.feature.ruang.domain.PlanProgressTopic
import com.gotest.app.feature.ruang.domain.PlanWeekProgress
import com.gotest.app.feature.ruang.ui.components.formatGrade
import com.gotest.app.feature.ruang.ui.components.serverWindow

/**
 * The four figures of a child's progress in one plan (web PlanProgressSummary): plan done, average score, this
 * week's status, and the active-day streak, two to a row.
 */
@Composable
fun PlanProgressSummary(
    progress: PlanProgress,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val completion = progress.completion
    val week = progress.week
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Figure(stringResource(R.string.progress_completion), Modifier.weight(1f)) { CompletionFigure(completion) }
            Figure(stringResource(R.string.progress_average), Modifier.weight(1f)) {
                BigNumber(progress.averageScore?.let(::formatGrade) ?: stringResource(R.string.progress_none))
                if (progress.averageScore == null) Note(stringResource(R.string.progress_average_none))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            val weekLabel =
                week.number?.let { stringResource(R.string.plan_week, it) }
                    ?: stringResource(R.string.plan_week_current)
            Figure(weekLabel, Modifier.weight(1f)) {
                val (label, tone) = weekStatus(week.status)
                val text =
                    if (week.status == "upcoming") {
                        stringResource(R.string.week_starts, shortDate(progress.plan.startsOn))
                    } else {
                        stringResource(label)
                    }
                GotestChip(text = text, tone = tone)
                if (week.counted > 0) Note(stringResource(R.string.progress_tasks_of, week.done, week.counted))
            }
            Figure(stringResource(R.string.progress_streak), Modifier.weight(1f)) {
                BigNumber(progress.streakDays.toString())
                Note(stringResource(R.string.progress_days))
            }
        }
    }
}

@Composable
private fun CompletionFigure(completion: PlanCompletion) {
    val percent = completion.percent
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        if (percent == null) {
            BigNumber(stringResource(R.string.progress_none))
            Note(stringResource(R.string.progress_completion_none))
        } else {
            BigNumber(stringResource(R.string.progress_percent, percent))
            GotestProgressBar(
                progress = percent / PERCENT,
                modifier = Modifier.fillMaxWidth(),
                color = GotestTheme.colors.bgBrandDark,
            )
            Note(stringResource(R.string.progress_tasks_of, completion.done, completion.counted))
        }
    }
}

@Composable
private fun Figure(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.medium)
                .padding(GotestTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        content()
    }
}

@Composable
private fun BigNumber(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = GotestTheme.colors.textPrimary,
    )
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

/** Topics in the server's order (red, yellow, not enough data, green), with their colour words (web TopicList). */
@Composable
fun TopicList(
    topics: List<PlanProgressTopic>,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (topics.isEmpty()) {
        Note(emptyText)
        return
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.medium),
    ) {
        topics.forEach { topic -> TopicRow(topic) }
    }
}

@Composable
private fun TopicRow(topic: PlanProgressTopic) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val (label, tone) = topicColor(topic.color)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(dotColor(tone), CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = topic.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Note(stringResource(R.string.progress_topic_counts, topic.taskCount, topic.questionCount))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            GotestChip(text = stringResource(label), tone = tone)
            if (topic.color != "gray") {
                Note(stringResource(R.string.progress_topic_average, formatGrade(topic.average)))
            }
        }
    }
}

@Composable
private fun dotColor(tone: GotestChipTone) =
    when (tone) {
        GotestChipTone.Red -> GotestTheme.colors.statusDanger
        GotestChipTone.Yellow -> GotestTheme.colors.statusWarning
        GotestChipTone.Green -> GotestTheme.colors.statusSuccess
        else -> GotestTheme.colors.borderStrong
    }

/** Riwayat minggu ini with where each score came from; a task taken off the plan says so (web HistoryList). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryList(
    history: List<PlanProgressHistory>,
    emptyText: String,
    family: Boolean,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        Note(emptyText)
        return
    }
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        history.forEach { row ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(
                            GotestTheme.sizes.strokeThin,
                            GotestTheme.colors.borderSubtle,
                            MaterialTheme.shapes.medium,
                        ).padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    GotestChip(text = stringResource(taskTypeLabel(row.type)), tone = GotestChipTone.Violet)
                    GotestChip(
                        text = if (row.deleted) stringResource(R.string.progress_deleted) else row.sourceLabel,
                        tone = GotestChipTone.White,
                    )
                }
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                )
                Note(historyDetails(row, family))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    val (status, tone) = taskStatus(row.status)
                    GotestChip(text = stringResource(status), tone = tone)
                    if (row.belowTarget) {
                        GotestChip(text = stringResource(R.string.task_below_target), tone = GotestChipTone.Yellow)
                    }
                }
                resultText(row.status, row.score, row.scoreLabel)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GotestTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun historyDetails(
    row: PlanProgressHistory,
    family: Boolean,
): String =
    listOfNotNull(
        row.topicName,
        if (row.status == "done" && row.completedAt != null) {
            stringResource(R.string.progress_done_at, serverWindow(row.completedAt))
        } else {
            stringResource(R.string.task_due, dayDate(row.dueOn))
        },
        recordedText(row.recordedBy, family),
    ).joinToString(" · ")

/** "Minggu 3 · 17 Nov sampai 23 Nov", the week the history covers; null without dates. */
@Composable
fun weekRangeText(week: PlanWeekProgress): String? {
    if (week.startsOn == null || week.endsOn == null) return null
    val starts = shortDate(week.startsOn)
    val ends = shortDate(week.endsOn)
    return week.number?.let { stringResource(R.string.plan_week_history_range, it, starts, ends) }
        ?: stringResource(R.string.plan_week_range, starts, ends)
}

private const val PERCENT = 100f

@Suppress("MagicNumber")
internal fun previewProgress(): PlanProgress =
    PlanProgress(
        plan = PlanProgressPlan(31, "Persiapan PTS Semester 1", "2026-11-09", "2026-12-06", "running"),
        completion = PlanCompletion(done = 3, counted = 5, percent = 60),
        week = PlanWeekProgress(2, "2026-11-16", "2026-11-22", done = 1, counted = 3, status = "slipping"),
        averageScore = 72.5,
        streakDays = 3,
        topics =
            listOf(
                PlanProgressTopic("a", "Pecahan", "red", 55.0, 10, 1),
                PlanProgressTopic("b", "Aljabar", "gray", 0.0, 0, 1),
            ),
        history =
            listOf(
                PlanProgressHistory(
                    taskId = 2,
                    title = "Kerjakan LKS halaman 12–15",
                    type = "external",
                    sourceLabel = "Dicatat manual",
                    topicName = "Pecahan",
                    workOn = "2026-11-19",
                    dueOn = "2026-11-20",
                    score = 60.0,
                    scoreLabel = "6 dari 10",
                    status = "done",
                    belowTarget = true,
                    completedAt = "2026-11-19T07:30:00Z",
                    recordedBy = "child",
                ),
            ),
    )

@Preview(showBackground = true)
@Composable
private fun PlanProgressPreview() {
    GotestTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PlanProgressSummary(previewProgress())
            TopicList(previewProgress().topics, emptyText = "")
            HistoryList(previewProgress().history, emptyText = "", family = false)
        }
    }
}
