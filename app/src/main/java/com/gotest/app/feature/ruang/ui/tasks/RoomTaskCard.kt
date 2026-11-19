package com.gotest.app.feature.ruang.ui.tasks

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestSegmentedControl
import com.gotest.app.core.designsystem.component.GotestTextField
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.RoomTask
import com.gotest.app.feature.ruang.domain.RoomTaskAction
import com.gotest.app.feature.ruang.domain.RoomTaskTarget
import com.gotest.app.feature.ruang.ui.components.formatGrade

/**
 * One Rencana Belajar task in the child's Mitra space (web RoomTaskCard): what it is, its status, and one action:
 * Mulai or Lanjutkan a quiz or a Gotest Latihan, see its result, or record an external task. A task that cannot be
 * worked on yet says why. [family]: "dicatat orang tua" in a Mitra Keluarga, "dicatat guru" elsewhere.
 * [practiceOpens]: a Gotest Latihan has a screen to go to (A2c); without one its button is left out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomTaskCard(
    task: RoomTask,
    family: Boolean,
    state: TaskActionState,
    onEvent: (TaskEvent) -> Unit,
    modifier: Modifier = Modifier,
    practiceOpens: Boolean = false,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, MaterialTheme.shapes.medium)
                .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            GotestChip(text = stringResource(taskTypeLabel(task.type)), tone = GotestChipTone.Violet)
            val (status, tone) = taskStatus(task.status)
            GotestChip(text = stringResource(status), tone = tone)
            if (task.belowTarget) {
                GotestChip(
                    text = stringResource(R.string.task_below_target),
                    tone = GotestChipTone.Yellow,
                )
            }
        }
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Text(text = details(task), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        resultText(task.status, task.score, task.scoreLabel)?.let { result ->
            val by = recordedText(task.recordedBy, family)
            Text(
                text = if (by != null) stringResource(R.string.task_result_with_by, result, by) else result,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
            )
        }
        task.blockedReason?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        state.errors[task.id]?.let {
            Text(
                text = it.ifBlank { stringResource(R.string.task_failed) },
                style = MaterialTheme.typography.bodySmall,
                color = colors.statusDanger,
            )
        }
        TaskButton(
            task = task,
            starting = state.startingId == task.id,
            practiceOpens = practiceOpens,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun TaskButton(
    task: RoomTask,
    starting: Boolean,
    practiceOpens: Boolean,
    onEvent: (TaskEvent) -> Unit,
) {
    val action = task.action ?: return
    val practice = task.type == RoomTask.TYPE_PRACTICE || action.target.type == RoomTaskTarget.PRACTICE_RESULT
    if (practice && !practiceOpens) return
    val (label, variant) =
        when (action.kind) {
            RoomTaskAction.START, RoomTaskAction.CONTINUE -> {
                val text =
                    when {
                        starting -> R.string.task_opening
                        action.kind == RoomTaskAction.CONTINUE -> R.string.task_continue
                        else -> R.string.task_start
                    }
                text to GotestButtonVariant.Filled
            }

            RoomTaskAction.RESULT -> {
                R.string.task_result to GotestButtonVariant.Plain
            }

            else -> {
                if (task.isDone) {
                    R.string.task_record_edit to GotestButtonVariant.Plain
                } else {
                    R.string.task_record to GotestButtonVariant.Filled
                }
            }
        }
    GotestButton(
        text = stringResource(label),
        onClick = { onEvent(TaskEvent.Act(task)) },
        modifier = Modifier.padding(top = GotestTheme.spacing.xs),
        variant = variant,
        size = GotestButtonSize.Small,
        enabled = !starting,
    )
}

/** Topic, target, minimum score, and due date, as RoomTaskCard joins them. */
@Composable
private fun details(task: RoomTask): String =
    listOfNotNull(
        task.topicName,
        task.targetQuestions?.takeIf { it > 0 }?.let { stringResource(R.string.task_questions, it) },
        stringResource(R.string.task_min_score, formatGrade(task.minScore))
            .takeIf { task.type != RoomTask.TYPE_EXTERNAL || (task.targetQuestions ?: 0) > 0 },
        stringResource(R.string.task_due, dayDate(task.dueOn)),
    ).joinToString(" · ")

@Composable
fun recordedText(
    recordedBy: String?,
    family: Boolean,
): String? =
    when (recordedBy) {
        "child" -> {
            stringResource(R.string.task_recorded_child)
        }

        "staff" -> {
            stringResource(
                R.string.task_recorded_staff,
                stringResource(if (family) R.string.task_staff_parent else R.string.task_staff_teacher),
            )
        }

        else -> {
            null
        }
    }

/** "Catat tugas selesai": 0–100, "Benar … dari …", or done without a score when the task has no target. */
@Composable
fun RecordDialog(
    form: RecordForm,
    onEvent: (TaskEvent) -> Unit,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Dialog(
        onDismissRequest = { onEvent(TaskEvent.CloseRecord) },
        properties = DialogProperties(dismissOnClickOutside = !form.busy, dismissOnBackPress = !form.busy),
    ) {
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(if (form.recorded) R.string.record_title_edit else R.string.record_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(text = form.task.title, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            val modes = form.modes
            GotestSegmentedControl(
                options = modes.map { stringResource(modeLabel(it)) },
                selectedIndex = modes.indexOf(form.mode).coerceAtLeast(0),
                onSelect = { onEvent(TaskEvent.ModeChange(modes[it])) },
            )
            RecordFields(form = form, onEvent = onEvent)
            GotestTextField(
                value = form.note,
                onValueChange = { onEvent(TaskEvent.FormChange(note = it.take(NOTE_MAX))) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.record_note),
                supportingText = form.fieldErrors["note"],
                isError = form.fieldErrors["note"] != null,
            )
            form.error?.let {
                Text(
                    text = it.ifBlank { stringResource(R.string.task_failed) },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.statusDanger,
                )
            }
            RecordButtons(form = form, onEvent = onEvent)
        }
    }
}

@Composable
private fun RecordFields(
    form: RecordForm,
    onEvent: (TaskEvent) -> Unit,
) {
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)
    when (form.mode) {
        RecordMode.Score -> {
            GotestTextField(
                value = form.score,
                onValueChange = { onEvent(TaskEvent.FormChange(score = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.record_score),
                placeholder = stringResource(R.string.record_score_hint),
                supportingText = form.fieldErrors["score"],
                isError = form.fieldErrors["score"] != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        RecordMode.Count -> {
            val error = form.fieldErrors["correct"] ?: form.fieldErrors["total"] ?: form.fieldErrors["score"]
            Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.record_correct), style = MaterialTheme.typography.bodyMedium)
                    GotestTextField(
                        value = form.correct,
                        onValueChange = { onEvent(TaskEvent.FormChange(correct = it.filter(Char::isDigit))) },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.record_correct_label),
                        isError = error != null,
                        keyboardOptions = number,
                    )
                    Text(text = stringResource(R.string.record_of), style = MaterialTheme.typography.bodyMedium)
                    GotestTextField(
                        value = form.total,
                        onValueChange = { onEvent(TaskEvent.FormChange(total = it.filter(Char::isDigit))) },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.record_total_label),
                        isError = error != null,
                        keyboardOptions = number,
                    )
                }
                error?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.statusDanger)
                }
            }
        }

        RecordMode.None -> {
            Text(
                text = stringResource(R.string.record_none_text),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun RecordButtons(
    form: RecordForm,
    onEvent: (TaskEvent) -> Unit,
) {
    val spacing = GotestTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.record_cancel),
                onClick = { onEvent(TaskEvent.CloseRecord) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = !form.busy,
            )
            GotestButton(
                text = stringResource(if (form.isSaving) R.string.record_saving else R.string.record_save),
                onClick = { onEvent(TaskEvent.Save) },
                modifier = Modifier.weight(1f),
                enabled = !form.busy,
            )
        }
        if (form.recorded) {
            GotestButton(
                text = stringResource(if (form.isRemoving) R.string.record_removing else R.string.record_remove),
                onClick = { onEvent(TaskEvent.Remove) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                contentColor = GotestTheme.colors.statusDanger,
                enabled = !form.busy,
            )
        }
    }
}

private fun modeLabel(mode: RecordMode): Int =
    when (mode) {
        RecordMode.Score -> R.string.record_mode_score
        RecordMode.Count -> R.string.record_mode_count
        RecordMode.None -> R.string.record_mode_none
    }

private const val NOTE_MAX = 200

@Suppress("MagicNumber")
internal fun previewTasks(): List<RoomTask> =
    listOf(
        RoomTask(
            id = 1,
            planId = 31,
            title = "Kuis Aljabar Dasar",
            type = RoomTask.TYPE_QUIZ,
            topicName = "Aljabar",
            targetQuestions = 6,
            minScore = 70.0,
            workOn = "2026-11-19",
            dueOn = "2026-11-20",
            status = "not_started",
            action = RoomTaskAction(RoomTaskAction.START, target = RoomTaskTarget("study_task_start", 1)),
        ),
        RoomTask(
            id = 2,
            planId = 31,
            title = "Kerjakan LKS halaman 12–15",
            type = RoomTask.TYPE_EXTERNAL,
            topicName = "Pecahan",
            targetQuestions = 10,
            minScore = 70.0,
            workOn = "2026-11-19",
            dueOn = "2026-11-20",
            status = RoomTask.STATUS_DONE,
            score = 60.0,
            scoreLabel = "6 dari 10",
            belowTarget = true,
            recordedBy = "child",
            action = RoomTaskAction(RoomTaskAction.RECORD, target = RoomTaskTarget("study_task_record", 2)),
        ),
        RoomTask(
            id = 3,
            planId = 31,
            title = "Kuis Pecahan",
            type = RoomTask.TYPE_QUIZ,
            topicName = "Pecahan",
            targetQuestions = 8,
            minScore = 70.0,
            workOn = "2026-11-30",
            dueOn = "2026-12-01",
            status = "not_started",
            blockedReason = "Tugas ini bisa dikerjakan mulai 30 November 2026.",
        ),
    )

@Preview(showBackground = true)
@Composable
private fun RoomTaskCardPreview() {
    GotestTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            previewTasks().forEach { RoomTaskCard(task = it, family = false, state = TaskActionState(), onEvent = {}) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordDialogPreview() {
    GotestTheme { RecordDialog(form = RecordForm.of(previewTasks()[1]), onEvent = {}) }
}
