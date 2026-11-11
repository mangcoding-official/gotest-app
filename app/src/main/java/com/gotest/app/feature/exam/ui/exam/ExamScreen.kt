package com.gotest.app.feature.exam.ui.exam

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.designsystem.component.GotestDialogContent
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.question.GotestQuestionOptions
import com.gotest.app.core.designsystem.component.question.GotestStimulusCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.exam.domain.hasStimulus
import com.gotest.app.feature.exam.ui.components.ExamBottomBar
import com.gotest.app.feature.exam.ui.components.ExamPreviewData
import kotlinx.coroutines.launch

@Composable
fun ExamRoute(
    onClose: () -> Unit,
    onShowResult: (attemptId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val submitError = stringResource(R.string.exam_submit_error)
    val saveError = stringResource(R.string.exam_save_error)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is ExamEffect.Submitted -> onShowResult(effect.attemptId)
            ExamEffect.SubmitFailed -> scope.launch { snackbarHostState.showSnackbar(submitError) }
            ExamEffect.SaveFailed -> scope.launch { snackbarHostState.showSnackbar(saveError) }
        }
    }
    ExamScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** The question engine for latihan and tryout (Pilihan Ganda, PG Kompleks, Benar-Salah). */
@Composable
fun ExamScreen(
    uiState: ExamUiState,
    onEvent: (ExamUiEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val success = uiState as? ExamUiState.Success
    Scaffold(
        modifier = modifier,
        topBar = { ExamHeader(state = success, onClose = onClose) },
        bottomBar = { success?.let { ExamControls(state = it, onEvent = onEvent) } },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GotestTheme.colors.bgApp,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        val contentModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        when (uiState) {
            ExamUiState.Loading -> {
                GotestLoadingView(modifier = contentModifier)
            }

            ExamUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(ExamUiEvent.Retry) }, modifier = contentModifier)
            }

            ExamUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.TaskSquare,
                    title = stringResource(R.string.exam_empty_title),
                    message = stringResource(R.string.exam_empty_message),
                    modifier = contentModifier,
                )
            }

            is ExamUiState.Success -> {
                QuestionPage(state = uiState, onEvent = onEvent, modifier = contentModifier)
                ExamOverlays(state = uiState, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ExamHeader(
    state: ExamUiState.Success?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgApp)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = GotestTheme.sizes.appBarHeight)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestIconButton(
                icon = GotestIcons.Linear.CloseCircle,
                contentDescription = stringResource(R.string.exam_cd_close),
                onClick = onClose,
            )
            if (state != null) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.exam_question_position,
                                state.currentIndex + 1,
                                state.questions.size,
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
                state.remainingSeconds?.let { TimerPill(remainingSeconds = it) }
            }
        }
        if (state != null) {
            ExamProgressLine(progress = state.progress)
        }
    }
}

@Composable
private fun TimerPill(
    remainingSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val clock = GotestFormat.clock(remainingSeconds)
    val description = stringResource(R.string.exam_cd_time_left, clock)
    Row(
        modifier =
            modifier
                .background(colors.bgBlueSoft, GotestTheme.shapes.full)
                .padding(horizontal = spacing.md, vertical = spacing.sm)
                .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO(design): the frame uses a clock glyph that GotestIcons does not have yet.
        Icon(
            painter = painterResource(GotestIcons.Linear.Calendar),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = colors.bgBlueDark,
        )
        Text(text = clock, style = MaterialTheme.typography.labelLarge, color = colors.bgBlueDark)
    }
}

/** Thin brand bar under the header showing the current position. */
@Composable
private fun ExamProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(targetValue = progress, label = "examProgress")
    val colors = GotestTheme.colors
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(GotestTheme.spacing.xs)
                .background(colors.bgGrey)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(colors.bgBrand),
        )
    }
}

@Composable
private fun QuestionPage(
    state: ExamUiState.Success,
    onEvent: (ExamUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val question = state.currentQuestion
    Box(modifier = modifier) {
        // A fresh scroll position for every question.
        key(question.id) {
            QuestionBody(
                question = question,
                number = state.currentIndex + 1,
                answer = state.currentAnswer,
                isMarked = state.isCurrentMarked,
                onEvent = onEvent,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.isSubmitting) {
            GotestLoadingView(modifier = Modifier.background(GotestTheme.colors.scrim))
        }
    }
}

@Composable
private fun QuestionBody(
    question: Question,
    number: Int,
    answer: QuestionAnswer,
    isMarked: Boolean,
    onEvent: (ExamUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.lg,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        QuestionMetaRow(
            number = number,
            topic = question.topic,
            isMarked = isMarked,
            onToggleMark = { onEvent(ExamUiEvent.ToggleMark) },
        )
        if (question.hasStimulus) {
            GotestStimulusCard(
                title = question.stimulusTitle,
                body = question.stimulusBody,
                imageModel = question.imageUrl,
            )
        }
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.bodyLarge,
            color = GotestTheme.colors.textPrimary,
        )
        question.instruction?.let { InstructionBox(text = it) }
        GotestQuestionOptions(
            question = question,
            answer = answer,
            onAnswerChange = { onEvent(ExamUiEvent.AnswerChanged(it)) },
        )
    }
}

@Composable
private fun QuestionMetaRow(
    number: Int,
    topic: String,
    isMarked: Boolean,
    onToggleMark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.exam_question_number, number),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            GotestChip(text = topic, tone = GotestChipTone.Violet)
        }
        MarkToggle(isMarked = isMarked, onToggle = onToggleMark)
    }
}

/** "Tandai" pill; marked questions turn yellow like the "Ditandai" legend. */
@Composable
private fun MarkToggle(
    isMarked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        checked = isMarked,
        onCheckedChange = { onToggle() },
        modifier = modifier,
        shape = GotestTheme.shapes.full,
        color = if (isMarked) colors.bgYellowSoft else colors.bgApp,
        contentColor = if (isMarked) colors.bgYellowDark else colors.textSecondary,
        border = BorderStroke(GotestTheme.sizes.strokeThin, if (isMarked) colors.bgYellow else colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm + spacing.xxs, vertical = spacing.xs + spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter =
                    painterResource(
                        if (isMarked) GotestIcons.Bold.ArchiveTick else GotestIcons.Linear.ArchiveAdd,
                    ),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconSm),
                tint = if (isMarked) colors.bgYellowDark else colors.iconDefault,
            )
            Text(
                text = stringResource(if (isMarked) R.string.exam_marked else R.string.exam_mark),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Grey instruction box of PG Kompleks and Benar-Salah. */
@Composable
private fun InstructionBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.small
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgSubtle, shape)
                .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, shape)
                .padding(GotestTheme.spacing.lg),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textPlaceholder,
    )
}

@Composable
private fun ExamControls(
    state: ExamUiState.Success,
    onEvent: (ExamUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isSubmitting
    ExamBottomBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GotestButton(
                text = stringResource(R.string.exam_previous),
                onClick = { onEvent(ExamUiEvent.Previous) },
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = enabled && !state.isFirst,
            )
            GotestIconButton(
                icon = GotestIcons.Linear.Menu,
                contentDescription = stringResource(R.string.exam_cd_navigator),
                onClick = { onEvent(ExamUiEvent.OpenNavigator) },
                modifier = Modifier.size(GotestTheme.sizes.buttonLarge),
                enabled = enabled,
            )
            GotestButton(
                text = stringResource(if (state.isLast) R.string.exam_finish else R.string.exam_next),
                onClick = { onEvent(if (state.isLast) ExamUiEvent.RequestFinish else ExamUiEvent.Next) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
        }
    }
}

/** Navigator sheet plus the finish confirmation (a sheet for tryouts, a dialog for latihan). */
@Composable
private fun ExamOverlays(
    state: ExamUiState.Success,
    onEvent: (ExamUiEvent) -> Unit,
) {
    val counts = state.counts
    if (state.isNavigatorOpen) {
        ExamNavigatorSheet(
            cells = state.navigatorCells,
            currentIndex = state.currentIndex,
            answeredCount = counts.answered,
            finishLabel =
                stringResource(
                    if (state.isTryout) R.string.exam_finish_tryout_button else R.string.exam_finish_practice_button,
                ),
            onSelect = { onEvent(ExamUiEvent.JumpTo(it)) },
            onFinish = { onEvent(ExamUiEvent.RequestFinish) },
            onDismiss = { onEvent(ExamUiEvent.DismissNavigator) },
        )
    }
    if (state.isFinishConfirmOpen && state.isTryout) {
        FinishTryoutSheet(
            counts = counts,
            onConfirm = { onEvent(ExamUiEvent.ConfirmFinish) },
            onDismiss = { onEvent(ExamUiEvent.DismissFinish) },
        )
    } else if (state.isFinishConfirmOpen) {
        GotestDialog(
            title = stringResource(R.string.exam_finish_practice_title),
            message = stringResource(R.string.exam_finish_practice_message, counts.answered, counts.total),
            confirmText = stringResource(R.string.exam_finish),
            onConfirm = { onEvent(ExamUiEvent.ConfirmFinish) },
            onDismissRequest = { onEvent(ExamUiEvent.DismissFinish) },
            icon = GotestIcons.Bold.TaskSquare,
            dismissText = stringResource(R.string.exam_cancel),
        )
    }
}

private fun previewState(
    fourth: Question,
    type: PackageType,
    fourthAnswer: QuestionAnswer,
    remainingSeconds: Int?,
): ExamUiState.Success {
    val questions = ExamPreviewData.twentyQuestions(fourth)
    val answers = ExamPreviewData.firstThreeAnswered(questions) + (questions[PREVIEW_INDEX].id to fourthAnswer)
    return ExamUiState.Success(
        attemptId = "att_preview",
        title = if (type == PackageType.Tryout) "Tryout UTBK-SNBT Session 1" else "Latihan September Session 1",
        type = type,
        questions = questions,
        answers = answers,
        markedIds = setOf(questions[PREVIEW_MARKED_INDEX].id),
        currentIndex = PREVIEW_INDEX,
        remainingSeconds = remainingSeconds,
    )
}

private const val PREVIEW_INDEX = 3
private const val PREVIEW_MARKED_INDEX = 6

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamScreenTryoutPreview() {
    GotestTheme {
        ExamScreen(
            uiState =
                previewState(
                    fourth = ExamPreviewData.routeQuestion,
                    type = PackageType.Tryout,
                    fourthAnswer = QuestionAnswer(selectedKeys = setOf("C")),
                    remainingSeconds = 2_432,
                ),
            onEvent = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamScreenComplexPreview() {
    GotestTheme {
        ExamScreen(
            uiState =
                previewState(
                    fourth = ExamPreviewData.complexQuestion,
                    type = PackageType.Practice,
                    fourthAnswer = QuestionAnswer(selectedKeys = setOf("A", "C")),
                    remainingSeconds = null,
                ),
            onEvent = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamScreenTrueFalsePreview() {
    GotestTheme {
        ExamScreen(
            uiState =
                previewState(
                    fourth = ExamPreviewData.trueFalseQuestion,
                    type = PackageType.Practice,
                    fourthAnswer = QuestionAnswer(statementAnswers = mapOf(1 to true, 2 to false)),
                    remainingSeconds = null,
                ).copy(markedIds = setOf("preview_4")),
            onEvent = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamScreenLoadingPreview() {
    GotestTheme {
        ExamScreen(uiState = ExamUiState.Loading, onEvent = {}, onClose = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamScreenErrorPreview() {
    GotestTheme {
        ExamScreen(uiState = ExamUiState.Error, onEvent = {}, onClose = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ExamScreenEmptyPreview() {
    GotestTheme {
        ExamScreen(uiState = ExamUiState.Empty, onEvent = {}, onClose = {})
    }
}

/** Frame 8644: the latihan finish dialog (shown here without its window). */
@Preview(showBackground = true, widthDp = 440)
@Composable
private fun ExamFinishPracticeDialogPreview() {
    GotestTheme {
        GotestDialogContent(
            title = "Selesaikan latihan?",
            message = "Cek semua soal sebelum menyelesaikan latihan. Kamu sudah menjawab 4 dari 20 soal.",
            confirmText = "Selesaikan",
            onConfirm = {},
            modifier = Modifier.padding(GotestTheme.spacing.xxl),
            icon = GotestIcons.Bold.TaskSquare,
            dismissText = "Batal",
        )
    }
}
