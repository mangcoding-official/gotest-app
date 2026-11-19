package com.gotest.app.feature.ruang.ui.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamAttemptInfo
import com.gotest.app.feature.ruang.domain.ExamBlock
import com.gotest.app.feature.ruang.domain.ExamContent
import com.gotest.app.feature.ruang.domain.ExamEntry
import com.gotest.app.feature.ruang.domain.ExamItem
import com.gotest.app.feature.ruang.domain.ExamItemType
import com.gotest.app.feature.ruang.domain.ExamOption
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone

@Composable
fun WorkDialogs(
    uiState: ExamWorkUiState,
    onEvent: (ExamWorkEvent) -> Unit,
) {
    when {
        uiState.otherDevice && uiState.page != null -> {
            OtherDeviceDialog(uiState = uiState, onEvent = onEvent)
        }

        uiState.submit != null -> {
            SubmitDialog(state = uiState.submit, onEvent = onEvent)
        }

        uiState.showExit -> {
            GotestDialog(
                title = stringResource(R.string.work_exit_title),
                message = stringResource(R.string.work_exit_message),
                confirmText = stringResource(R.string.work_exit_confirm),
                onConfirm = { onEvent(ExamWorkEvent.ConfirmExit) },
                onDismissRequest = { onEvent(ExamWorkEvent.DismissExit) },
                dismissText = stringResource(R.string.work_keep_working),
            )
        }

        uiState.violationWarning != null -> {
            GotestDialog(
                title = stringResource(R.string.work_violation_title, uiState.violationWarning, uiState.violationLimit),
                message = stringResource(R.string.work_violation_message, uiState.violationLimit),
                confirmText = stringResource(R.string.work_keep_working),
                onConfirm = { onEvent(ExamWorkEvent.DismissViolation) },
                onDismissRequest = { onEvent(ExamWorkEvent.DismissViolation) },
            )
        }
    }
}

/** Cannot be closed: continue here, or go back to the list (Work.tsx). */
@Composable
private fun OtherDeviceDialog(
    uiState: ExamWorkUiState,
    onEvent: (ExamWorkEvent) -> Unit,
) {
    WorkDialog(title = stringResource(R.string.work_other_device_title), onDismissRequest = {}) {
        Body(stringResource(R.string.work_other_device_message))
        val error =
            uiState.takeOverMessage ?: stringResource(R.string.work_take_over_failed).takeIf { uiState.takeOverFailed }
        error?.let { NoticeBox(text = it, tone = NoticeTone.Danger) }
        GotestButton(
            text = stringResource(if (uiState.isTakingOver) R.string.work_taking_over else R.string.work_take_over),
            onClick = { onEvent(ExamWorkEvent.TakeOver) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isTakingOver && !uiState.timeUp,
        )
        GotestButton(
            text = stringResource(R.string.work_back_to_list),
            onClick = { onEvent(ExamWorkEvent.ConfirmExit) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

@Composable
private fun SubmitDialog(
    state: SubmitDialogState,
    onEvent: (ExamWorkEvent) -> Unit,
) {
    WorkDialog(
        title = stringResource(R.string.work_submit_title),
        onDismissRequest = { if (!state.isSubmitting) onEvent(ExamWorkEvent.DismissSubmit) },
    ) {
        if (state.unanswered != null) {
            NoticeBox(text = stringResource(R.string.work_submit_unanswered, state.unanswered))
        } else {
            Body(stringResource(R.string.work_submit_all_answered))
        }
        Body(stringResource(R.string.work_submit_note))
        val error =
            state.message
                ?: stringResource(R.string.work_submit_unsent).takeIf { state.unsent }
                ?: stringResource(R.string.work_submit_failed).takeIf { state.failed }
        error?.let { NoticeBox(text = it, tone = NoticeTone.Danger) }
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text =
                    stringResource(
                        if (state.unanswered !=
                            null
                        ) {
                            R.string.work_submit_check
                        } else {
                            R.string.ruang_cancel
                        },
                    ),
                onClick = { onEvent(ExamWorkEvent.DismissSubmit) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = !state.isSubmitting,
            )
            GotestButton(
                text =
                    stringResource(
                        if (state.isSubmitting) R.string.work_submitting else R.string.work_submit_confirm,
                    ),
                onClick = { onEvent(ExamWorkEvent.ConfirmSubmit) },
                modifier = Modifier.weight(1f),
                enabled = !state.isSubmitting,
            )
        }
    }
}

@Composable
private fun WorkDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnClickOutside = false)) {
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md), content = content)
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

/** A small paper for previews (sample numbers). */
@Suppress("MagicNumber")
internal fun previewWorkState(): ExamWorkUiState {
    val single =
        ExamItem(55, 1, ExamItemType.SINGLE, "<p>Keliling segitiga sama sisi dengan sisi 8 cm adalah ....</p>")
    val matching = ExamItem(56, 2, ExamItemType.MATCHING, "<p>Pasangkan setiap bentuk dengan hasilnya.</p>")
    val items =
        mapOf(
            55L to
                WorkItem(
                    single,
                    ExamContent.Choice(
                        listOf(ExamOption("a", "16 cm", null), ExamOption("b", "24 cm", null)),
                        multiple = false,
                    ),
                ),
            56L to
                WorkItem(
                    matching,
                    ExamContent.Matching(
                        left = listOf(ExamEntry("l1", "Dua pangkat tiga"), ExamEntry("l2", "Akar 81")),
                        right = listOf(ExamEntry("r2", "9"), ExamEntry("r1", "8")),
                    ),
                ),
        )
    return ExamWorkUiState(
        isLoading = false,
        page =
            ExamWorkPage(
                organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
                title = "PTS Matematika Kelas 8",
                attempt = ExamAttemptInfo(9, "in_progress", "2026-12-01T02:30:00Z", "2026-12-01T01:30:00Z", 1, 3, "t"),
                blocks = listOf(ExamBlock("single", listOf(55L, 56L))),
            ),
        items = items,
        answers = mapOf(55L to ExamAnswer.Single("b"), 56L to ExamAnswer.Matching(mapOf("l1" to "r1"))),
        secondsLeft = 5_399,
        violations = 1,
        violationLimit = 3,
    )
}
