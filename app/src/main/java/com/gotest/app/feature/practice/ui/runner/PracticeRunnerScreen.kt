package com.gotest.app.feature.practice.ui.runner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.practice.domain.ActiveQuestion
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeItem
import com.gotest.app.feature.practice.domain.ResponseKind
import com.gotest.app.feature.practice.domain.isAnswered
import com.gotest.app.feature.practice.domain.responseKind
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.work.ExamImage

@Composable
fun PracticeRunnerRoute(
    packageName: String,
    onOpenResult: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeRunnerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            PracticeRunnerEffect.OpenResult -> onOpenResult()
            PracticeRunnerEffect.Leave -> onLeave()
        }
    }
    // Leaving asks first, as the web's leave guard does; a stopped Latihan has nothing left to save.
    BackHandler {
        viewModel.onEvent(
            if (uiState.stopped !=
                null
            ) {
                PracticeRunnerEvent.Leave
            } else {
                PracticeRunnerEvent.ShowDialog(RunnerDialog.Exit)
            },
        )
    }
    PracticeRunnerScreen(
        uiState = uiState,
        packageName = packageName,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
fun PracticeRunnerScreen(
    uiState: PracticeRunnerUiState,
    packageName: String,
    onEvent: (PracticeRunnerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        topBar = {
            RunnerHeader(uiState = uiState, packageName = packageName, onExit = {
                onEvent(PracticeRunnerEvent.ShowDialog(RunnerDialog.Exit))
            })
        },
        bottomBar = { if (uiState.attempt != null) RunnerFooter(uiState = uiState, onEvent = onEvent) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            uiState.timedOutNotice?.let {
                NoticeBox(text = stringResource(R.string.runner_timed_out_notice, it), tone = NoticeTone.Info)
            }
            uiState.message?.let { NoticeBox(text = messageText(it), tone = NoticeTone.Danger) }
            QuestionStage(uiState = uiState, onEvent = onEvent)
            if (uiState.attempt != null) NavigationPanel(uiState = uiState, onEvent = onEvent)
        }
    }
    RunnerDialogs(uiState = uiState, packageName = packageName, onEvent = onEvent)
}

@Composable
private fun RunnerHeader(
    uiState: PracticeRunnerUiState,
    packageName: String,
    onExit: () -> Unit,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.runner_kicker),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
            )
            if (packageName.isNotBlank()) {
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                    maxLines = 2,
                )
            }
        }
        uiState.secondsLeft?.let { seconds ->
            val perQuestion = uiState.attempt?.timerMode == PracticeAttempt.TIMER_PER_QUESTION
            GotestChip(
                text =
                    stringResource(
                        if (perQuestion) R.string.runner_question_time_left else R.string.runner_time_left,
                        clockText(seconds),
                    ),
                tone = if (seconds == 0L) GotestChipTone.Red else GotestChipTone.Violet,
            )
        }
        GotestButton(
            text = stringResource(R.string.runner_exit),
            onClick = onExit,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

/** Web `formatExamDuration`: minutes and seconds, "90:00". */
fun clockText(seconds: Long): String = "%02d:%02d".format(seconds / SECONDS_IN_MINUTE, seconds % SECONDS_IN_MINUTE)

@Composable
private fun QuestionStage(
    uiState: PracticeRunnerUiState,
    onEvent: (PracticeRunnerEvent) -> Unit,
) {
    val question = uiState.question
    val item = uiState.activeItem
    when {
        uiState.questionReady && question != null && item != null -> {
            QuestionCard(uiState = uiState, question = question, item = item, onEvent = onEvent)
        }

        uiState.activationFailedFor != null && uiState.activationFailedFor == uiState.selectedItemId -> {
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                Note(stringResource(R.string.runner_cannot_show))
                GotestButton(
                    text = stringResource(R.string.runner_retry),
                    onClick = { onEvent(PracticeRunnerEvent.RetryActivation) },
                    variant = GotestButtonVariant.Plain,
                )
            }
        }

        else -> {
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                GotestLoadingView()
                Note(stringResource(R.string.runner_preparing))
            }
        }
    }
}

@Composable
private fun QuestionCard(
    uiState: PracticeRunnerUiState,
    question: ActiveQuestion,
    item: PracticeItem,
    onEvent: (PracticeRunnerEvent) -> Unit,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.runner_question, uiState.currentIndex + 1),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        question.content.stimulus?.let { stimulus ->
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgSubtle) {
                RichText(
                    content = stimulus.text,
                    isHtml = true,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                )
                mediaUrl(stimulus.mediaPath)?.let { ExamImage(url = it, description = stimulus.name.orEmpty()) }
            }
        }
        RichText(
            content = question.content.prompt.orEmpty(),
            isHtml = true,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        mediaUrl(question.content.mediaPath)?.let {
            ExamImage(url = it, description = stringResource(R.string.runner_question_image))
        }
        val kind = responseKind(question.questionType ?: item.questionType)
        PracticeResponse(
            kind = kind,
            content = question.content,
            answer = uiState.activeAnswer,
            enabled = !uiState.itemClosed && !uiState.timeUp && !uiState.isSubmitting,
            onEvent = onEvent,
        )
        if (kind == ResponseKind.Statement) StatementProgress(uiState)
        if (uiState.attempt?.linear == true) Note(stringResource(R.string.runner_linear_note))
        if (uiState.itemClosed || uiState.timeUp) Note(closedText(uiState))
    }
}

@Composable
private fun StatementProgress(uiState: PracticeRunnerUiState) {
    val total =
        uiState.question
            ?.content
            ?.statements
            ?.size ?: 0
    val answered = (uiState.activeAnswer as? AnswerValue.Statement)?.labels?.count { it != null } ?: 0
    Note(
        stringResource(R.string.runner_statement_progress, answered, total) +
            stringResource(
                if (answered <
                    total
                ) {
                    R.string.runner_statement_incomplete
                } else {
                    R.string.runner_statement_complete
                },
            ),
    )
}

@Composable
private fun closedText(uiState: PracticeRunnerUiState): String {
    val attempt = uiState.attempt
    if (uiState.timeUp &&
        attempt?.timerMode == PracticeAttempt.TIMER_PACKAGE
    ) {
        return stringResource(R.string.runner_practice_time_up)
    }
    val first =
        if (uiState.activeItem?.timedOut == true || uiState.timeUp) {
            stringResource(R.string.runner_question_time_up)
        } else {
            stringResource(R.string.runner_question_skipped)
        }
    val second =
        when {
            attempt?.linear != true -> stringResource(R.string.runner_pick_other)
            uiState.nextItem != null -> stringResource(R.string.runner_go_next)
            else -> stringResource(R.string.runner_finish_to_see)
        }
    return first + second
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

/** Topics and the question grid with "Selesaikan Latihan" (web ExamRunnerShell sidebar), under the question. */
@Composable
private fun NavigationPanel(
    uiState: PracticeRunnerUiState,
    onEvent: (PracticeRunnerEvent) -> Unit,
) {
    // A linear Latihan shows where the student is, but the order is the server's (navReadOnly).
    val open = uiState.attempt?.linear != true && !uiState.isSaving
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        TopicRow(uiState = uiState, onGo = { itemId -> if (open) onEvent(PracticeRunnerEvent.GoTo(itemId)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.runner_nav_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.runner_nav_filled, uiState.answeredCount, uiState.items.size),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        QuestionGrid(uiState = uiState, onGo = { item ->
            if (open &&
                !item.closed
            ) {
                onEvent(PracticeRunnerEvent.GoTo(item.id))
            }
        })
        GotestButton(
            text =
                stringResource(
                    if (uiState.isSubmitting) R.string.runner_finishing else R.string.runner_finish_practice,
                ),
            onClick = { onEvent(PracticeRunnerEvent.ShowDialog(RunnerDialog.Submit)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicRow(
    uiState: PracticeRunnerUiState,
    onGo: (Long) -> Unit,
) {
    val items = uiState.items
    val topics = items.map { it.subjectName.orEmpty() }.distinct()
    if (topics.isEmpty()) return
    val general = stringResource(R.string.practice_general_subject)
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Text(
            text = stringResource(R.string.runner_topics),
            style = MaterialTheme.typography.titleSmall,
            color = GotestTheme.colors.textPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            topics.forEachIndexed { index, topic ->
                GotestFilterPill(
                    text = "${index + 1}. ${topic.ifBlank { general }}",
                    selected = uiState.activeItem?.subjectName.orEmpty() == topic,
                    onClick = { items.firstOrNull { it.subjectName.orEmpty() == topic }?.let { onGo(it.id) } },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionGrid(
    uiState: PracticeRunnerUiState,
    onGo: (PracticeItem) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
    ) {
        uiState.items.forEach { item ->
            val answered = isAnswered(item, uiState.answers[item.questionId])
            val state =
                when {
                    answered -> R.string.runner_nav_answered
                    item.closed -> R.string.runner_nav_locked
                    else -> R.string.runner_nav_empty
                }
            val description = stringResource(state, item.displayOrder)
            GotestFilterPill(
                text = item.displayOrder.toString(),
                selected = item.id == uiState.selectedItemId || answered,
                onClick = { onGo(item) },
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

@Composable
private fun RunnerFooter(
    uiState: PracticeRunnerUiState,
    onEvent: (PracticeRunnerEvent) -> Unit,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        val finish: @Composable () -> Unit = {
            GotestButton(
                text = stringResource(R.string.runner_finish),
                onClick = { onEvent(PracticeRunnerEvent.ShowDialog(RunnerDialog.Submit)) },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving && !uiState.isSubmitting,
            )
        }
        if (uiState.attempt?.linear == true) {
            if (uiState.canFinishHere) {
                finish()
            } else {
                GotestButton(
                    text = stringResource(R.string.runner_advance),
                    onClick = { onEvent(PracticeRunnerEvent.Advance) },
                    modifier = Modifier.weight(1f),
                    enabled =
                        uiState.nextItem != null && uiState.questionReady && !uiState.isSaving &&
                            uiState.advancingTo == null,
                )
            }
        } else {
            GotestButton(
                text = stringResource(R.string.runner_previous),
                onClick = { onEvent(PracticeRunnerEvent.Previous) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = uiState.previousItem != null && !uiState.isSaving,
            )
            if (uiState.canFinishHere) {
                finish()
            } else {
                GotestButton(
                    text = stringResource(R.string.runner_next),
                    onClick = { onEvent(PracticeRunnerEvent.Next) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.nextItem != null && !uiState.isSaving,
                )
            }
        }
    }
}

@Composable
private fun messageText(message: RunnerMessage): String =
    when (message) {
        is RunnerMessage.Server -> {
            message.text
        }

        is RunnerMessage.Refused -> {
            stringResource(
                R.string.runner_answer_refused,
                message.number,
                message.reason ?: stringResource(R.string.runner_answer_refused_fallback),
            )
        }

        RunnerMessage.Offline -> {
            stringResource(R.string.runner_offline)
        }

        RunnerMessage.LoadFailed -> {
            stringResource(R.string.runner_load_failed)
        }

        RunnerMessage.OpenFailed -> {
            stringResource(R.string.runner_open_failed)
        }

        RunnerMessage.SaveFailed -> {
            stringResource(R.string.runner_save_failed)
        }

        RunnerMessage.SubmitFailed -> {
            stringResource(R.string.runner_submit_failed)
        }
    }

@Composable
private fun RunnerDialogs(
    uiState: PracticeRunnerUiState,
    packageName: String,
    onEvent: (PracticeRunnerEvent) -> Unit,
) {
    val dismiss = { onEvent(PracticeRunnerEvent.ShowDialog(RunnerDialog.None)) }
    uiState.stopped?.let { reason ->
        val leave = { onEvent(PracticeRunnerEvent.Leave) }
        RunnerDialogCard(title = stringResource(R.string.runner_stopped_title), onDismiss = leave) {
            if (reason.isNotBlank()) Body(reason)
            GotestButton(
                text = stringResource(R.string.runner_stopped_back),
                onClick = leave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    uiState.finishedAfterRejection?.let { number ->
        RunnerDialogCard(title = stringResource(R.string.runner_finished_title), onDismiss = {
            onEvent(PracticeRunnerEvent.OpenResult)
        }) {
            Body(
                stringResource(
                    if (uiState.attempt?.timerEnabled ==
                        true
                    ) {
                        R.string.runner_finished_timer
                    } else {
                        R.string.runner_finished_manual
                    },
                    number,
                ),
            )
            GotestButton(
                text = stringResource(R.string.runner_see_result),
                onClick = { onEvent(PracticeRunnerEvent.OpenResult) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    when (uiState.dialog) {
        RunnerDialog.Skip -> {
            SkipDialog(isSaving = uiState.isSaving, onEvent = onEvent, onDismiss = dismiss)
        }

        RunnerDialog.Submit -> {
            SubmitDialog(
                uiState = uiState,
                packageName = packageName,
                onEvent = onEvent,
                onDismiss = dismiss,
            )
        }

        RunnerDialog.Exit -> {
            ExitDialog(uiState = uiState, onEvent = onEvent, onDismiss = dismiss)
        }

        RunnerDialog.None -> {}
    }
}

@Composable
private fun SkipDialog(
    isSaving: Boolean,
    onEvent: (PracticeRunnerEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    RunnerDialogCard(title = stringResource(R.string.runner_skip_title), onDismiss = onDismiss) {
        Body(stringResource(R.string.runner_skip_message))
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.runner_skip_back),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            GotestButton(
                text = stringResource(R.string.runner_skip_confirm),
                onClick = { onEvent(PracticeRunnerEvent.ConfirmSkip) },
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
            )
        }
    }
}

@Composable
private fun SubmitDialog(
    uiState: PracticeRunnerUiState,
    packageName: String,
    onEvent: (PracticeRunnerEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val total = uiState.items.size
    val unanswered = (total - uiState.answeredCount).coerceAtLeast(0)
    RunnerDialogCard(
        title = stringResource(R.string.runner_submit_title),
        subtitle = packageName,
        onDismiss = onDismiss,
    ) {
        if (unanswered > 0) {
            NoticeBox(
                text =
                    stringResource(R.string.runner_submit_unanswered_title, unanswered) + "\n" +
                        stringResource(R.string.runner_submit_unanswered, uiState.answeredCount, total),
            )
        }
        Body(stringResource(R.string.runner_submit_message))
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text =
                    stringResource(
                        if (unanswered >
                            0
                        ) {
                            R.string.runner_submit_check
                        } else {
                            R.string.runner_submit_cancel
                        },
                    ),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = !uiState.isSubmitting,
            )
            GotestButton(
                text =
                    stringResource(
                        if (uiState.isSubmitting) R.string.runner_processing else R.string.runner_submit_confirm,
                    ),
                onClick = { onEvent(PracticeRunnerEvent.ConfirmSubmit) },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSubmitting,
            )
        }
    }
}

@Composable
private fun ExitDialog(
    uiState: PracticeRunnerUiState,
    onEvent: (PracticeRunnerEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val attempt = uiState.attempt
    val timerRuns = attempt?.timerEnabled == true && attempt.timerMode != PracticeAttempt.TIMER_NONE
    val timerText =
        when {
            !timerRuns -> {
                ""
            }

            attempt.timerMode == PracticeAttempt.TIMER_PER_QUESTION -> {
                stringResource(
                    R.string.runner_exit_question_timer,
                )
            }

            else -> {
                stringResource(R.string.runner_exit_package_timer)
            }
        }
    RunnerDialogCard(title = stringResource(R.string.runner_exit_title), onDismiss = onDismiss) {
        Body(stringResource(R.string.runner_exit_message) + timerText)
        GotestButton(
            text = stringResource(R.string.runner_keep_working),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
        GotestButton(
            text = stringResource(R.string.runner_save_exit),
            onClick = { onEvent(PracticeRunnerEvent.SaveAndExit) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            enabled = !uiState.isSubmitting && !uiState.isSaving,
        )
        GotestButton(
            text = stringResource(if (uiState.isSubmitting) R.string.runner_finishing else R.string.runner_finish_see),
            onClick = { onEvent(PracticeRunnerEvent.ConfirmSubmit) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            enabled = !uiState.isSubmitting,
        )
    }
}

@Composable
private fun RunnerDialogCard(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
            subtitle?.takeIf { it.isNotBlank() }?.let { Body(it) }
            Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) { content() }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

private const val SECONDS_IN_MINUTE = 60
