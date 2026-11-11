package com.gotest.app.feature.multiplayer.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.component.question.GotestQuestionOptions
import com.gotest.app.core.designsystem.component.question.GotestStimulusCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbBottomBar
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewOverlay
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewSheet
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import com.gotest.app.feature.multiplayer.ui.components.gameTypeLabel

@Composable
fun GamePlayRoute(
    onFinish: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GamePlayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is GamePlayEffect.Submitted -> onFinish(effect.code)
        }
    }
    GamePlayScreen(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun GamePlayScreen(
    uiState: GamePlayUiState,
    onEvent: (GamePlayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Leaving mid-game means finishing it: ask first.
    BackHandler(enabled = uiState is GamePlayUiState.Success) { onEvent(GamePlayUiEvent.OpenFinish) }
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgApp)) {
        when (uiState) {
            GamePlayUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            }

            GamePlayUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(GamePlayUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
                )
            }

            is GamePlayUiState.Success -> {
                PlayContent(state = uiState, onEvent = onEvent)
                PlaySheetHost(state = uiState, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun PlayContent(
    state: GamePlayUiState.Success,
    onEvent: (GamePlayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PlayHeader(state = state, onClose = { onEvent(GamePlayUiEvent.OpenFinish) })
        GotestProgressBar(
            progress = (state.currentIndex + 1).toFloat() / state.questions.size,
            color = GotestTheme.colors.bgBrand,
            height = GotestTheme.spacing.xs,
        )
        QuestionBody(state = state, onEvent = onEvent, modifier = Modifier.weight(1f))
        MbBottomBar {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GotestButton(
                    text = stringResource(R.string.multiplayer_play_previous),
                    onClick = { onEvent(GamePlayUiEvent.Previous) },
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                    enabled = !state.isFirst,
                )
                GotestIconButton(
                    icon = GotestIcons.Linear.Menu,
                    contentDescription = stringResource(R.string.multiplayer_cd_question_navigator),
                    onClick = { onEvent(GamePlayUiEvent.OpenNavigator) },
                )
                GotestButton(
                    text =
                        stringResource(
                            if (state.isLast) R.string.multiplayer_play_finish else R.string.multiplayer_play_next,
                        ),
                    onClick = { onEvent(GamePlayUiEvent.Next) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** "Main Bareng • Duel / Soal 4 dari 20 • vs Ardy Pratama" with the close button and the timer. */
@Composable
private fun PlayHeader(
    state: GamePlayUiState.Success,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val position = state.currentIndex + 1
    val total = state.questions.size
    val opponent = state.opponentName
    val subtitle =
        if (state.type == GameType.Duel && opponent != null) {
            stringResource(R.string.multiplayer_play_progress_versus, position, total, opponent)
        } else {
            stringResource(
                R.string.multiplayer_play_progress_room,
                position,
                total,
                pluralStringResource(R.plurals.multiplayer_player_count, state.playerCount, state.playerCount),
            )
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = GotestTheme.sizes.appBarHeight)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestIconButton(
            icon = GotestIcons.Linear.CloseCircle,
            contentDescription = stringResource(R.string.multiplayer_cd_exit_game),
            onClick = onClose,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(R.string.multiplayer_play_title, gameTypeLabel(state.type)),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TimerPill(seconds = state.remainingSeconds)
    }
}

@Composable
private fun TimerPill(
    seconds: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val clock = GotestFormat.clock(seconds)
    val description = stringResource(R.string.multiplayer_cd_time_left, clock)
    Row(
        modifier =
            modifier
                .background(colors.bgBlueSoft, GotestTheme.shapes.full)
                .padding(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.sm)
                .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs + GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The design's clock glyph is not in GotestIcons; the calendar stands in (as in frame 19162).
        Icon(
            painter = painterResource(GotestIcons.Linear.Calendar),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconSm),
            tint = colors.bgBlue,
        )
        Text(text = clock, style = MaterialTheme.typography.labelLarge, color = colors.bgBlueDark)
    }
}

@Composable
private fun QuestionBody(
    state: GamePlayUiState.Success,
    onEvent: (GamePlayUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val question = state.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.lg,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (state.isOffline) OfflineAlert(numbers = state.unsavedQuestionNumbers)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_play_question, question.number),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            GotestChip(text = question.topic, tone = GotestChipTone.Violet)
        }
        if (question.stimulusTitle != null || question.stimulusBody != null || question.imageUrl != null) {
            GotestStimulusCard(
                title = question.stimulusTitle,
                body = question.stimulusBody,
                imageModel = question.imageUrl,
            )
        }
        Text(text = question.prompt, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        question.instruction?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        GotestQuestionOptions(
            question = question,
            answer = state.answerFor(question),
            onAnswerChange = { onEvent(GamePlayUiEvent.Answer(it)) },
        )
    }
}

/** Yellow "Koneksi terputus" banner (frame 19377). */
@Composable
private fun OfflineAlert(
    numbers: List<Int>,
    modifier: Modifier = Modifier,
) {
    val message =
        when (numbers.size) {
            0 -> stringResource(R.string.multiplayer_play_offline_none)
            1 -> stringResource(R.string.multiplayer_play_offline_one, numbers[0])
            2 -> stringResource(R.string.multiplayer_play_offline_two, numbers[0], numbers[1])
            else -> pluralStringResource(R.plurals.multiplayer_play_offline_many, numbers.size, numbers.size)
        }
    MbAlert(
        type = MbAlertType.Warning,
        title = stringResource(R.string.multiplayer_play_offline_title),
        message = message,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaySheetHost(
    state: GamePlayUiState.Success,
    onEvent: (GamePlayUiEvent) -> Unit,
) {
    val sheet = state.sheet ?: return
    GotestBottomSheet(onDismissRequest = { onEvent(GamePlayUiEvent.DismissSheet) }) {
        when (sheet) {
            PlaySheet.ConfirmFinish -> {
                FinishSheetContent(
                    answered = state.answeredCount,
                    empty = state.emptyCount,
                    isSubmitting = state.isSubmitting,
                    onConfirm = { onEvent(GamePlayUiEvent.ConfirmFinish) },
                    onDismiss = { onEvent(GamePlayUiEvent.DismissSheet) },
                )
            }

            PlaySheet.Navigator -> {
                NavigatorSheetContent(
                    state = state,
                    onSelect = { onEvent(GamePlayUiEvent.GoTo(it)) },
                    onFinish = { onEvent(GamePlayUiEvent.OpenFinish) },
                )
            }
        }
    }
}

private val previewState =
    GamePlayUiState.Success(
        type = GameType.Duel,
        opponentName = "Ardy Pratama",
        playerCount = 2,
        questions = MultiplayerPreviewData.questions,
        currentIndex = 3,
        answers = MultiplayerPreviewData.previewAnswers,
        remainingSeconds = 761,
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GamePlayScreenPreview() {
    GotestTheme { GamePlayScreen(uiState = previewState, onEvent = {}) }
}

/** Frame 19377: the fake connectivity flag is only switched off here. */
@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GamePlayOfflinePreview() {
    GotestTheme {
        GamePlayScreen(
            uiState = previewState.copy(isOffline = true, unsavedQuestionNumbers = listOf(3, 4)),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GamePlayFinishSheetPreview() {
    GotestTheme {
        MbPreviewOverlay(
            screen = { GamePlayScreen(uiState = previewState, onEvent = {}) },
            overlayAlignment = Alignment.BottomCenter,
        ) {
            MbPreviewSheet {
                FinishSheetContent(answered = 17, empty = 3, isSubmitting = false, onConfirm = {}, onDismiss = {})
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GamePlayLoadingPreview() {
    GotestTheme { GamePlayScreen(uiState = GamePlayUiState.Loading, onEvent = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GamePlayErrorPreview() {
    GotestTheme { GamePlayScreen(uiState = GamePlayUiState.Error, onEvent = {}) }
}
