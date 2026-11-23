package com.gotest.app.feature.multiplayer.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.richtext.RichText
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.isAnswered
import com.gotest.app.feature.multiplayer.ui.PlayError
import com.gotest.app.feature.multiplayer.ui.PlayEvent
import com.gotest.app.feature.multiplayer.ui.PlayModal
import com.gotest.app.feature.multiplayer.ui.PlayState
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbBadgeTone
import com.gotest.app.feature.multiplayer.ui.components.MbDialog
import com.gotest.app.feature.multiplayer.ui.components.formatClock
import com.gotest.app.feature.multiplayer.ui.components.modeLabel
import com.gotest.app.feature.multiplayer.ui.hub.Body
import com.gotest.app.feature.practice.domain.responseKind
import com.gotest.app.feature.practice.ui.runner.PracticeResponse
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerEvent
import com.gotest.app.feature.practice.ui.runner.mediaUrl
import com.gotest.app.feature.ruang.data.listNumbers
import com.gotest.app.feature.ruang.ui.work.ExamImage

/** "3, 2, 1, Mulai!": every player counts down to the same server start (web StartCountdown). */
@Composable
internal fun CountdownContent(
    secondsLeft: Long?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(
        modifier = modifier.fillMaxSize().background(colors.bgBrandDark),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.multiplayer_countdown_kicker),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textOnBrand,
        )
        val left = secondsLeft ?: 0
        Text(
            text = if (left > 0) left.toString() else stringResource(R.string.multiplayer_countdown_go),
            style = MaterialTheme.typography.displayLarge,
            color = colors.textOnBrand,
        )
        Text(
            text = stringResource(R.string.multiplayer_countdown_note),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textOnBrand,
        )
    }
}

/** Answering (web PlayScreen): game info, question, navigation, and the server's timer. */
@Composable
internal fun PlayContent(
    game: GameState,
    play: PlayState,
    secondsLeft: Long?,
    onEvent: (PlayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeUp = secondsLeft == 0L
    Scaffold(
        modifier = modifier,
        containerColor = GotestTheme.colors.bgSubtle,
        topBar = {
            PlayHeader(
                game = game,
                secondsLeft = secondsLeft,
                onExit = { onEvent(PlayEvent.Modal(PlayModal.Leave)) },
            )
        },
        bottomBar = { PlayFooter(play = play, onEvent = onEvent) },
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
            play.error?.let {
                MbAlert(
                    type = MbAlertType.Warning,
                    title = stringResource(R.string.multiplayer_play_not_sent_title),
                    message =
                        stringResource(
                            if (it == PlayError.FinishFailed) {
                                R.string.multiplayer_play_finish_failed
                            } else {
                                R.string.multiplayer_play_leave_failed
                            },
                        ),
                )
            }
            if (play.unsentNumbers.isNotEmpty()) {
                MbAlert(
                    type = MbAlertType.Warning,
                    title = stringResource(R.string.multiplayer_play_offline_title),
                    message =
                        stringResource(
                            R.string.multiplayer_play_offline_message,
                            listNumbers(play.unsentNumbers),
                        ),
                )
            }
            play.question?.let { QuestionCard(play = play, question = it, timeUp = timeUp, onEvent = onEvent) }
            InfoCard(game)
            Navigator(play = play, onEvent = onEvent)
        }
    }
    PlayDialogs(play = play, onEvent = onEvent)
}

@Composable
private fun PlayHeader(
    game: GameState,
    secondsLeft: Long?,
    onExit: () -> Unit,
) {
    val colors = GotestTheme.colors
    val mode = modeLabel(game.mode)
    val kicker =
        game.organization?.let { stringResource(R.string.multiplayer_play_kicker_space, it.name, mode) }
            ?: stringResource(R.string.multiplayer_play_kicker, mode)
    val rival = game.players.firstOrNull { !it.isYou }
    val subtitle =
        if (game.isDuel && rival != null) {
            stringResource(R.string.multiplayer_play_versus, game.subjectName, rival.name)
        } else {
            stringResource(R.string.multiplayer_play_players, game.subjectName, game.players.size)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.bgApp)
                .statusBarsPadding()
                .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = kicker, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            Text(text = subtitle, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.multiplayer_play_timer),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Text(
                text = formatClock(secondsLeft ?: 0),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
        }
        GotestButton(
            text = stringResource(R.string.multiplayer_play_exit),
            onClick = onExit,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

@Composable
private fun QuestionCard(
    play: PlayState,
    question: GameQuestion,
    timeUp: Boolean,
    onEvent: (PlayEvent) -> Unit,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.multiplayer_play_question, play.index + 1),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        question.content.stimulus?.let { stimulus ->
            GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = colors.bgBlueSoft, showBorder = false) {
                RichText(
                    content = stimulus.text,
                    isHtml = true,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                )
                mediaUrl(stimulus.mediaPath)?.let {
                    ExamImage(url = it, description = stringResource(R.string.multiplayer_play_stimulus_image))
                }
            }
        }
        RichText(
            content = question.content.prompt.orEmpty(),
            isHtml = true,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
        )
        mediaUrl(question.content.mediaPath)?.let {
            ExamImage(url = it, description = stringResource(R.string.multiplayer_play_question_image))
        }
        PracticeResponse(
            kind = responseKind(question.questionType),
            content = question.content,
            answer = play.answers[question.id],
            enabled = !timeUp && !play.finishing,
            onEvent = { event -> event.toPlay()?.let(onEvent) },
        )
        if (timeUp) {
            MbAlert(
                type = MbAlertType.Warning,
                title = stringResource(R.string.multiplayer_play_time_up_title),
                message = stringResource(R.string.multiplayer_play_time_up_message),
            )
        }
    }
}

private fun PracticeRunnerEvent.toPlay(): PlayEvent? =
    when (this) {
        is PracticeRunnerEvent.SelectOption -> PlayEvent.Select(index)
        is PracticeRunnerEvent.ToggleOption -> PlayEvent.Toggle(index)
        is PracticeRunnerEvent.StatementAnswer -> PlayEvent.Statement(row, label)
        else -> null
    }

@Composable
private fun InfoCard(game: GameState) {
    val colors = GotestTheme.colors
    val rows =
        listOf(
            stringResource(R.string.multiplayer_detail_type) to
                stringResource(
                    if (game.isDuel) R.string.multiplayer_mode_detail_duel else R.string.multiplayer_mode_detail_room,
                ),
            stringResource(R.string.multiplayer_detail_question_count) to
                stringResource(R.string.multiplayer_questions, game.questionCount),
            stringResource(R.string.multiplayer_detail_duration) to
                stringResource(R.string.multiplayer_minutes, game.durationMinutes),
            stringResource(R.string.multiplayer_detail_code) to game.code,
        )
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.multiplayer_play_info_title),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        rows.forEach { (term, value) ->
            Row {
                Body(term)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
        Text(
            text = stringResource(R.string.multiplayer_play_info_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textBrandDark,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgBrandSoft, MaterialTheme.shapes.small)
                    .padding(GotestTheme.spacing.md),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Navigator(
    play: PlayState,
    onEvent: (PlayEvent) -> Unit,
) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_play_navigator),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Body(stringResource(R.string.multiplayer_play_filled, play.answeredCount, play.questions.size))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            play.questions.forEachIndexed { index, question ->
                val number = index + 1
                val answered = question.isAnsweredIn(play)
                val description =
                    when {
                        question.id in play.unsent -> stringResource(R.string.multiplayer_cd_nav_unsent, number)
                        answered -> stringResource(R.string.multiplayer_cd_nav_answered, number)
                        else -> stringResource(R.string.multiplayer_cd_nav_empty, number)
                    }
                GotestFilterPill(
                    text = number.toString(),
                    selected = index == play.index || answered,
                    onClick = { onEvent(PlayEvent.GoTo(index)) },
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
        }
        GotestButton(
            text =
                stringResource(
                    if (play.finishing) R.string.multiplayer_play_sending else R.string.multiplayer_play_finish,
                ),
            onClick = { onEvent(PlayEvent.Modal(PlayModal.Finish)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !play.finishing,
        )
    }
}

private fun GameQuestion.isAnsweredIn(play: PlayState) = isAnswered(play.answers[id])

@Composable
private fun PlayFooter(
    play: PlayState,
    onEvent: (PlayEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgApp)
                .navigationBarsPadding()
                .padding(horizontal = GotestTheme.spacing.screenHorizontal, vertical = GotestTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        GotestButton(
            text = stringResource(R.string.multiplayer_play_previous),
            onClick = { onEvent(PlayEvent.GoTo(play.index - 1)) },
            modifier = Modifier.weight(1f),
            variant = GotestButtonVariant.Plain,
            enabled = play.index > 0,
        )
        if (play.index < play.questions.size - 1) {
            GotestButton(
                text = stringResource(R.string.multiplayer_play_next),
                onClick = { onEvent(PlayEvent.GoTo(play.index + 1)) },
                modifier = Modifier.weight(1f),
            )
        } else {
            GotestButton(
                text = stringResource(R.string.multiplayer_play_finish),
                onClick = { onEvent(PlayEvent.Modal(PlayModal.Finish)) },
                modifier = Modifier.weight(1f),
                enabled = !play.finishing,
            )
        }
    }
}

@Composable
private fun PlayDialogs(
    play: PlayState,
    onEvent: (PlayEvent) -> Unit,
) {
    val close = { onEvent(PlayEvent.Modal(PlayModal.None)) }
    when (play.modal) {
        PlayModal.Finish -> {
            MbDialog(
                icon = GotestIcons.Bold.TickCircle,
                tone = MbBadgeTone.Green,
                title = stringResource(R.string.multiplayer_finish_title),
                message =
                    if (play.unansweredCount > 0) {
                        stringResource(R.string.multiplayer_finish_message_some, play.unansweredCount)
                    } else {
                        stringResource(R.string.multiplayer_finish_message_all)
                    },
                confirmText = stringResource(R.string.multiplayer_finish_confirm),
                onConfirm = { onEvent(PlayEvent.Finish) },
                onDismissRequest = close,
                dismissText = stringResource(R.string.multiplayer_back),
            )
        }

        PlayModal.Leave -> {
            MbDialog(
                icon = GotestIcons.Bold.CloseCircle,
                tone = MbBadgeTone.Red,
                title = stringResource(R.string.multiplayer_leave_title),
                message = stringResource(R.string.multiplayer_leave_message),
                confirmText = stringResource(R.string.multiplayer_leave_confirm),
                onConfirm = { onEvent(PlayEvent.Leave) },
                onDismissRequest = close,
                dismissText = stringResource(R.string.multiplayer_back),
            )
        }

        PlayModal.None -> {}
    }
}
