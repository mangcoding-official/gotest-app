package com.gotest.app.feature.multiplayer.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.question.GotestQuestionCell
import com.gotest.app.core.designsystem.component.question.QuestionCellState
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.ui.components.MbIconTile
import com.gotest.app.feature.multiplayer.ui.components.MbLegendItem
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewSheet
import com.gotest.app.feature.multiplayer.ui.components.MbStatTile
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerDimens
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData

/** "Selesai sekarang?" (frame 19162). */
@Composable
fun FinishSheetContent(
    answered: Int,
    empty: Int,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier.padding(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                bottom = spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        MbIconTile(
            icon = GotestIcons.Bold.InfoCircle,
            container = colors.bgYellowSoft,
            tint = colors.statusWarning,
            size = GotestTheme.sizes.dialogIcon,
            iconSize = GotestTheme.sizes.iconXl,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(R.string.multiplayer_finish_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text =
                    if (empty > 0) {
                        pluralStringResource(R.plurals.multiplayer_finish_message, empty, empty)
                    } else {
                        stringResource(R.string.multiplayer_finish_message_all_answered)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MbStatTile(
                value = answered.toString(),
                label = stringResource(R.string.multiplayer_finish_answered),
                container = colors.bgGreenSoft,
                content = colors.bgGreenDark,
                modifier = Modifier.weight(1f),
                horizontal = true,
            )
            MbStatTile(
                value = empty.toString(),
                label = stringResource(R.string.multiplayer_finish_empty),
                container = colors.bgSubtle,
                content = colors.textPrimary,
                modifier = Modifier.weight(1f),
                horizontal = true,
            )
        }
        Column(
            modifier = Modifier.padding(top = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GotestButton(
                text = stringResource(R.string.multiplayer_finish_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            )
            GotestButton(
                text = stringResource(R.string.multiplayer_back),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
            )
        }
    }
}

/** "Navigasi Soal", laid out like the exam's navigator sheet. */
@Composable
fun NavigatorSheetContent(
    state: GamePlayUiState.Success,
    onSelect: (index: Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier.padding(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                bottom = spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_navigator_title),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.multiplayer_navigator_filled, state.answeredCount, state.questions.size),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            state.questions.indices.chunked(MultiplayerDimens.NAVIGATOR_GRID_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    row.forEach { index ->
                        val question = state.questions[index]
                        GotestQuestionCell(
                            number = question.number,
                            state =
                                when {
                                    index == state.currentIndex -> QuestionCellState.Current
                                    state.answers[question.id]?.isEmpty == false -> QuestionCellState.Answered
                                    else -> QuestionCellState.Empty
                                },
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f).heightIn(min = GotestTheme.sizes.questionCell),
                        )
                    }
                    repeat(MultiplayerDimens.NAVIGATOR_GRID_COLUMNS - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MbLegendItem(
                colors.bgGreen,
                stringResource(R.string.multiplayer_navigator_answered),
                swatchSize = MultiplayerDimens.legendSwatch,
            )
            MbLegendItem(
                colors.borderFocus,
                stringResource(R.string.multiplayer_navigator_current),
                swatchSize = MultiplayerDimens.legendSwatch,
            )
            MbLegendItem(
                colors.bgGrey,
                stringResource(R.string.multiplayer_navigator_empty),
                swatchSize = MultiplayerDimens.legendSwatch,
            )
        }
        GotestButton(
            text = stringResource(R.string.multiplayer_play_finish),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigatorSheetPreview() {
    GotestTheme {
        MbPreviewSheet {
            NavigatorSheetContent(
                state =
                    GamePlayUiState.Success(
                        type = GameType.Duel,
                        opponentName = "Ardy Pratama",
                        playerCount = 2,
                        questions = MultiplayerPreviewData.questions,
                        currentIndex = 3,
                        answers = MultiplayerPreviewData.previewAnswers,
                        remainingSeconds = 761,
                    ),
                onSelect = {},
                onFinish = {},
            )
        }
    }
}
