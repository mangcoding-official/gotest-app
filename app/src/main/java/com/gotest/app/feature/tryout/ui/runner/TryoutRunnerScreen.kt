package com.gotest.app.feature.tryout.ui.runner

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
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.ResponseKind
import com.gotest.app.feature.practice.domain.responseKind
import com.gotest.app.feature.practice.ui.runner.PracticeResponse
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerEvent
import com.gotest.app.feature.practice.ui.runner.clockText
import com.gotest.app.feature.practice.ui.runner.mediaUrl
import com.gotest.app.feature.ruang.ui.components.NoticeBox
import com.gotest.app.feature.ruang.ui.components.NoticeTone
import com.gotest.app.feature.ruang.ui.work.ExamImage
import com.gotest.app.feature.tryout.domain.TryoutItem
import com.gotest.app.feature.tryout.domain.TryoutSectionSummary

@Composable
fun TryoutRunnerRoute(
    packageName: String,
    onOpenResult: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TryoutRunnerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            TryoutRunnerEffect.OpenResult -> onOpenResult()
            TryoutRunnerEffect.Leave -> onLeave()
        }
    }
    BackHandler { viewModel.onEvent(TryoutRunnerEvent.ShowDialog(TryoutDialog.Exit)) }
    TryoutRunnerScreen(uiState = uiState, packageName = packageName, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun TryoutRunnerScreen(
    uiState: TryoutRunnerUiState,
    packageName: String,
    onEvent: (TryoutRunnerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        topBar = {
            RunnerHeader(uiState = uiState, packageName = packageName, onExit = {
                onEvent(TryoutRunnerEvent.ShowDialog(TryoutDialog.Exit))
            })
        },
        bottomBar = { if (uiState.section != null) RunnerFooter(uiState = uiState, onEvent = onEvent) },
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
            uiState.message?.let { NoticeBox(text = messageText(it), tone = NoticeTone.Danger) }
            if (uiState.timeUp && uiState.section != null) {
                NoticeBox(
                    text =
                        stringResource(
                            if (uiState.isLastSection) R.string.trun_time_up_result else R.string.trun_time_up_next,
                        ),
                )
            }
            Stage(uiState = uiState, onEvent = onEvent)
            if (uiState.section != null) NavigationPanel(uiState = uiState, onEvent = onEvent)
        }
    }
    RunnerDialogs(uiState = uiState, packageName = packageName, onEvent = onEvent)
}

@Composable
private fun RunnerHeader(
    uiState: TryoutRunnerUiState,
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
                text = stringResource(R.string.trun_kicker),
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
            GotestChip(
                text = stringResource(R.string.runner_time_left, clockText(seconds)),
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

@Composable
private fun Stage(
    uiState: TryoutRunnerUiState,
    onEvent: (TryoutRunnerEvent) -> Unit,
) {
    val next = uiState.attempt?.nextSection
    val item = uiState.activeItem
    val content = item?.question?.content
    when {
        uiState.section == null && next != null -> {
            SectionIntro(uiState = uiState, next = next, onEvent = onEvent)
        }

        item != null && content != null -> {
            QuestionCard(uiState = uiState, item = item, onEvent = onEvent)
        }

        else -> {
            GotestCard(modifier = Modifier.fillMaxWidth()) {
                GotestLoadingView()
                Note(stringResource(R.string.trun_preparing))
            }
        }
    }
}

/** The section introduction: the timer starts only when the participant begins. */
@Composable
private fun SectionIntro(
    uiState: TryoutRunnerUiState,
    next: TryoutSectionSummary,
    onEvent: (TryoutRunnerEvent) -> Unit,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trun_intro_of, next.orderNumber, uiState.attempt?.sections?.size ?: 0),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textBrandDark,
        )
        Text(text = next.title, style = MaterialTheme.typography.titleLarge, color = GotestTheme.colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            IntroFact(
                stringResource(R.string.trun_intro_questions),
                stringResource(R.string.trun_intro_questions_value, next.questionCount),
                Modifier.weight(1f),
            )
            IntroFact(
                stringResource(R.string.trun_intro_duration),
                sectionDuration(next.durationSeconds ?: 0),
                Modifier.weight(1f),
            )
        }
        Note(stringResource(R.string.trun_intro_text))
        GotestButton(
            text =
                stringResource(
                    if (uiState.isBeginning) R.string.session_starting else R.string.trun_begin,
                    next.orderNumber,
                ),
            onClick = { onEvent(TryoutRunnerEvent.Begin) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isBeginning,
        )
    }
}

@Composable
private fun IntroFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        verticalSpacing = GotestTheme.spacing.xs,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
    }
}

@Composable
private fun sectionDuration(seconds: Int): String {
    val minutes = seconds / SECONDS_PER_MINUTE
    val rest = seconds % SECONDS_PER_MINUTE
    return if (rest ==
        0
    ) {
        stringResource(R.string.session_duration_minutes, minutes)
    } else {
        stringResource(R.string.session_duration_both, minutes, rest)
    }
}

@Composable
private fun QuestionCard(
    uiState: TryoutRunnerUiState,
    item: TryoutItem,
    onEvent: (TryoutRunnerEvent) -> Unit,
) {
    val colors = GotestTheme.colors
    val content = item.question.content ?: return
    val flagged = uiState.flags[item.id] ?: false
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.runner_question, uiState.currentIndex + 1),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        content.stimulus?.let { stimulus ->
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgSubtle) {
                RichText(
                    content = stimulus.text,
                    isHtml = true,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                )
                mediaUrl(
                    stimulus.mediaPath,
                )?.let { ExamImage(url = it, description = stringResource(R.string.trun_stimulus_image)) }
            }
        }
        RichText(
            content = content.prompt.orEmpty(),
            isHtml = true,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        mediaUrl(
            content.mediaPath,
        )?.let { ExamImage(url = it, description = stringResource(R.string.runner_question_image)) }
        val kind = responseKind(item.question.questionType)
        PracticeResponse(
            kind = kind,
            content = content,
            answer = uiState.answers[item.id],
            enabled = !uiState.isSaving && !uiState.timeUp,
            onEvent = { event -> event.toTryout()?.let(onEvent) },
        )
        GotestButton(
            text = stringResource(if (flagged) R.string.trun_unflag else R.string.trun_flag),
            onClick = { onEvent(TryoutRunnerEvent.ToggleFlag) },
            variant = if (flagged) GotestButtonVariant.Filled else GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
            enabled = !uiState.timeUp,
        )
        Note(stringResource(R.string.trun_skip_note))
        if (kind == ResponseKind.Statement) {
            val total = content.statements.size
            val answered = (uiState.answers[item.id] as? AnswerValue.Statement)?.labels?.count { it != null } ?: 0
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
    }
}

/** The shared answer areas speak Latihan events; the Try Out runner reads them as its own. */
private fun PracticeRunnerEvent.toTryout(): TryoutRunnerEvent? =
    when (this) {
        is PracticeRunnerEvent.SelectOption -> TryoutRunnerEvent.SelectOption(index)
        is PracticeRunnerEvent.ToggleOption -> TryoutRunnerEvent.ToggleOption(index)
        is PracticeRunnerEvent.StatementAnswer -> TryoutRunnerEvent.StatementAnswer(row, label)
        else -> null
    }

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
}

/** Subtests as topics, the question grid with ragu-ragu marks, and the section's finishing button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NavigationPanel(
    uiState: TryoutRunnerUiState,
    onEvent: (TryoutRunnerEvent) -> Unit,
) {
    val colors = GotestTheme.colors
    val locked = uiState.isSaving || uiState.hasUnsaved
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.runner_topics),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            uiState.attempt?.sections.orEmpty().forEachIndexed { index, section ->
                GotestFilterPill(
                    text = "${index + 1}. ${section.title}",
                    selected =
                        section.status == TryoutSectionSummary.STARTED,
                    onClick = {},
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.runner_nav_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.runner_nav_filled, uiState.answeredCount, uiState.items.size),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            uiState.items.forEachIndexed { index, item ->
                val answered = uiState.answered(item)
                val flagged = uiState.flags[item.id] ?: false
                val base =
                    stringResource(
                        if (answered) R.string.runner_nav_answered else R.string.runner_nav_empty,
                        index + 1,
                    )
                val description = if (flagged) stringResource(R.string.trun_nav_flagged, base) else base
                GotestFilterPill(
                    text = if (flagged) "${index + 1}?" else "${index + 1}",
                    selected = item.id == uiState.activeItem?.id || answered,
                    onClick = { if (!locked) onEvent(TryoutRunnerEvent.GoTo(item.id)) },
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
        GotestButton(
            text = stringResource(finishLabel(uiState)),
            onClick = { onEvent(TryoutRunnerEvent.ShowDialog(TryoutDialog.Action)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving && !uiState.isFinishing,
        )
    }
}

private fun finishLabel(uiState: TryoutRunnerUiState): Int =
    when {
        uiState.isFinishing -> R.string.runner_processing
        uiState.isLastSection -> R.string.trun_finish_tryout
        else -> R.string.trun_finish_section
    }

@Composable
private fun RunnerFooter(
    uiState: TryoutRunnerUiState,
    onEvent: (TryoutRunnerEvent) -> Unit,
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
        GotestButton(
            text = stringResource(R.string.runner_previous),
            onClick = { onEvent(TryoutRunnerEvent.Previous) },
            modifier = Modifier.weight(1f),
            variant = GotestButtonVariant.Plain,
            enabled = uiState.previousItem != null && !uiState.isSaving,
        )
        val last = uiState.isLastQuestion
        GotestButton(
            text =
                stringResource(
                    when {
                        !last -> R.string.runner_next
                        uiState.isFinishing -> R.string.runner_processing
                        uiState.isLastSection -> R.string.trun_finish_tryout
                        else -> R.string.trun_next_section
                    },
                ),
            onClick = {
                if (last) {
                    onEvent(
                        TryoutRunnerEvent.ShowDialog(TryoutDialog.Action),
                    )
                } else {
                    onEvent(TryoutRunnerEvent.Next)
                }
            },
            modifier = Modifier.weight(1f),
            enabled =
                !uiState.isSaving && !uiState.hasUnsaved &&
                    (if (last) !uiState.isFinishing else uiState.nextItem != null),
        )
    }
}

@Composable
private fun messageText(message: TryoutMessage): String =
    when (message) {
        is TryoutMessage.Server -> {
            message.text
        }

        is TryoutMessage.Refused -> {
            stringResource(
                R.string.runner_answer_refused,
                message.number,
                message.reason ?: stringResource(R.string.runner_answer_refused_fallback),
            )
        }

        is TryoutMessage.NotSaved -> {
            stringResource(
                R.string.trun_not_saved,
                when {
                    message.offline -> stringResource(R.string.runner_offline)
                    else -> message.reason ?: stringResource(R.string.trun_not_saved_fallback)
                },
            )
        }

        TryoutMessage.Offline -> {
            stringResource(R.string.runner_offline)
        }

        TryoutMessage.StatusFailed -> {
            stringResource(R.string.trun_status_failed)
        }

        TryoutMessage.FlagFailed -> {
            stringResource(R.string.trun_flag_failed)
        }

        TryoutMessage.StaleDrafts -> {
            stringResource(R.string.trun_stale)
        }

        TryoutMessage.KeepAnswering -> {
            stringResource(R.string.trun_keep_answering)
        }
    }

@Composable
private fun RunnerDialogs(
    uiState: TryoutRunnerUiState,
    packageName: String,
    onEvent: (TryoutRunnerEvent) -> Unit,
) {
    val dismiss = { onEvent(TryoutRunnerEvent.ShowDialog(TryoutDialog.None)) }
    when (uiState.dialog) {
        TryoutDialog.Action -> {
            ActionDialog(uiState = uiState, packageName = packageName, onEvent = onEvent, onDismiss = dismiss)
        }

        TryoutDialog.Exit -> {
            ExitDialog(uiState = uiState, packageName = packageName, onEvent = onEvent, onDismiss = dismiss)
        }

        TryoutDialog.End -> {
            EndDialog(uiState = uiState, onEvent = onEvent, onDismiss = dismiss)
        }

        TryoutDialog.None -> {}
    }
}

@Composable
private fun ActionDialog(
    uiState: TryoutRunnerUiState,
    packageName: String,
    onEvent: (TryoutRunnerEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val section = uiState.section
    val total = uiState.items.size
    val answered = uiState.answeredCount
    val unanswered = (total - answered).coerceAtLeast(0)
    val flagged = uiState.items.count { uiState.flags[it.id] == true }
    val last = uiState.isLastSection
    DialogCard(
        title =
            if (last) {
                stringResource(R.string.trun_finish_tryout)
            } else {
                stringResource(
                    R.string.trun_action_section,
                    section?.orderNumber ?: 0,
                )
            },
        subtitle = section?.title ?: packageName,
        onDismiss = onDismiss,
    ) {
        if (flagged > 0) Body(stringResource(R.string.trun_flagged_count, flagged))
        if (unanswered > 0) {
            NoticeBox(
                text =
                    stringResource(R.string.runner_submit_unanswered_title, unanswered) + "\n" +
                        stringResource(
                            if (last) R.string.trun_unanswered_last else R.string.trun_unanswered_section,
                            answered,
                            total,
                        ),
            )
        }
        Body(
            if (last) {
                stringResource(
                    R.string.trun_confirm_last,
                )
            } else {
                stringResource(R.string.trun_confirm_section, section?.title.orEmpty())
            },
        )
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
                enabled = !uiState.isFinishing,
            )
            GotestButton(
                text = stringResource(if (last) R.string.trun_yes_finish else R.string.trun_yes_section),
                onClick = { onEvent(TryoutRunnerEvent.ConfirmAction) },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isFinishing,
            )
        }
    }
}

@Composable
private fun ExitDialog(
    uiState: TryoutRunnerUiState,
    packageName: String,
    onEvent: (TryoutRunnerEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val running = uiState.section != null
    DialogCard(
        title = stringResource(R.string.trun_exit_title),
        subtitle = stringResource(R.string.trun_exit_subtitle, packageName),
        onDismiss = onDismiss,
    ) {
        Body(stringResource(if (running) R.string.trun_exit_running else R.string.trun_exit_not_started))
        GotestButton(
            text = stringResource(R.string.runner_keep_working),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
        GotestButton(
            text =
                stringResource(
                    if (running) R.string.trun_exit_leave_running else R.string.trun_exit_leave_not_started,
                ),
            onClick = { onEvent(TryoutRunnerEvent.Leave) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
        GotestButton(
            text = stringResource(R.string.trun_exit_end),
            onClick = { onEvent(TryoutRunnerEvent.ShowDialog(TryoutDialog.End)) },
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
            contentColor = GotestTheme.colors.statusDanger,
            enabled = !uiState.isFinishing,
        )
    }
}

@Composable
private fun EndDialog(
    uiState: TryoutRunnerUiState,
    onEvent: (TryoutRunnerEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val unanswered = if (uiState.section != null) uiState.items.count { !uiState.answered(it) } else 0
    val text =
        when {
            uiState.endCancels() -> stringResource(R.string.trun_end_cancels)
            unanswered > 0 -> stringResource(R.string.trun_end_locks_empty, uiState.remainingSections(), unanswered)
            else -> stringResource(R.string.trun_end_locks, uiState.remainingSections())
        }
    DialogCard(title = stringResource(R.string.trun_end_title), onDismiss = onDismiss) {
        Body(text)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.runner_submit_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = !uiState.isFinishing,
            )
            GotestButton(
                text =
                    stringResource(
                        if (uiState.isFinishing) R.string.runner_processing else R.string.trun_end_confirm,
                    ),
                onClick = { onEvent(TryoutRunnerEvent.ConfirmEnd) },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isFinishing,
            )
        }
    }
}

@Composable
private fun DialogCard(
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

private const val SECONDS_PER_MINUTE = 60
