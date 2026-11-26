package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.ui.JoinForm
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbBadgeTone
import com.gotest.app.feature.multiplayer.ui.components.MbCodeInput
import com.gotest.app.feature.multiplayer.ui.components.MbDialog
import com.gotest.app.feature.multiplayer.ui.components.MbInfoTile
import com.gotest.app.feature.multiplayer.ui.components.gameTitle
import com.gotest.app.feature.multiplayer.ui.components.modeLabel
import com.gotest.app.feature.multiplayer.ui.components.problemText

/** "Gabung dengan kode" (web JoinModal): look the code up, show the game, then join; or say why not. */
@Composable
internal fun JoinGameDialog(
    join: JoinForm,
    onCode: (String) -> Unit,
    onSubmit: () -> Unit,
    onResume: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        GotestCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.multiplayer_join_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = GotestTheme.colors.textPrimary,
            )
            Body(stringResource(R.string.multiplayer_join_sheet_body))
            MbCodeInput(
                value = join.code,
                onValueChange = onCode,
                onDone = onSubmit,
                isError = join.problem?.kind == GameProblem.NOT_FOUND,
            )
            join.found?.let { FoundGame(it) }
            join.problem?.let { problem ->
                val (title, type) = joinNotice(problem)
                MbAlert(type = type, title = stringResource(title), message = problemText(problem))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                GotestButton(
                    text = stringResource(R.string.multiplayer_create_cancel),
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                    enabled = !join.busy,
                )
                if (join.blocking) {
                    val busy = join.problem?.kind == GameProblem.BUSY
                    GotestButton(
                        text =
                            stringResource(
                                if (busy) R.string.multiplayer_back_to_game else R.string.multiplayer_back_to_hub,
                            ),
                        onClick = if (busy) onResume else onClose,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    GotestButton(
                        text = stringResource(submitLabel(join)),
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = !join.busy,
                    )
                }
            }
            if (join.found != null) Body(stringResource(R.string.multiplayer_join_sheet_note))
        }
    }
}

private fun submitLabel(join: JoinForm): Int =
    when {
        join.busy && join.found != null -> R.string.multiplayer_join_joining
        join.busy -> R.string.multiplayer_join_searching
        join.found != null -> R.string.multiplayer_join_action
        else -> R.string.multiplayer_join_search
    }

/** Web ERROR_TITLES: the title and color of the box for each refusal. */
private fun joinNotice(problem: GameProblem): Pair<Int, MbAlertType> =
    when (problem.kind) {
        GameProblem.NOT_FOUND -> R.string.multiplayer_join_not_found_title to MbAlertType.Error
        "started" -> R.string.multiplayer_join_started_title to MbAlertType.Warning
        "full" -> R.string.multiplayer_join_full_title to MbAlertType.Warning
        "closed" -> R.string.multiplayer_join_closed_title to MbAlertType.Error
        GameProblem.BUSY -> R.string.multiplayer_busy_title to MbAlertType.Info
        "not_member" -> R.string.multiplayer_join_not_member_title to MbAlertType.Warning
        else -> R.string.multiplayer_join_failed_title to MbAlertType.Error
    }

@Composable
private fun FoundGame(game: GameState) {
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = gameTitle(game.mode, game.subjectName),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            when {
                game.running -> {
                    GotestChip(
                        stringResource(R.string.multiplayer_status_running),
                        tone = GotestChipTone.Green,
                    )
                }

                game.waiting -> {
                    GotestChip(
                        stringResource(R.string.multiplayer_status_waiting),
                        tone = GotestChipTone.Yellow,
                    )
                }

                else -> {
                    GotestChip(stringResource(R.string.multiplayer_status_finished), tone = GotestChipTone.Green)
                }
            }
        }
        Body(stringResource(R.string.multiplayer_created_by, game.hostName))
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_question_count),
                value = stringResource(R.string.multiplayer_questions, game.questionCount),
                modifier = Modifier.weight(1f),
            )
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_duration),
                value = stringResource(R.string.multiplayer_minutes, game.durationMinutes),
                modifier = Modifier.weight(1f),
            )
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_players),
                value = stringResource(R.string.multiplayer_count_of, game.players.size, game.maxPlayers),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A free account tries to create a game (web PremiumModal). Payments are hidden in the app (K-AN2), so without
 * [onUpgrade] the dialog keeps only the first sentence and "Gabung dengan Kode".
 */
@Composable
internal fun PremiumDialog(
    onJoin: () -> Unit,
    onUpgrade: (() -> Unit)?,
    onClose: () -> Unit,
) {
    MbDialog(
        icon = GotestIcons.Bold.Award,
        tone = MbBadgeTone.Yellow,
        title = stringResource(R.string.multiplayer_premium_dialog_title),
        message =
            stringResource(
                if (onUpgrade != null) {
                    R.string.multiplayer_premium_dialog_message
                } else {
                    R.string.multiplayer_premium_dialog_message_no_upgrade
                },
            ),
        confirmText =
            stringResource(
                if (onUpgrade != null) R.string.multiplayer_premium_dialog_upgrade else R.string.multiplayer_join_title,
            ),
        onConfirm = onUpgrade ?: onJoin,
        onDismissRequest = onClose,
        dismissText = if (onUpgrade != null) stringResource(R.string.multiplayer_join_title) else null,
        onDismiss = if (onUpgrade != null) onJoin else onClose,
    )
}

/** One game at a time: the viewer is already in another one (web BusyModal). */
@Composable
internal fun BusyDialog(
    game: GameState?,
    onResume: () -> Unit,
    onClose: () -> Unit,
) {
    val label =
        game?.let {
            stringResource(R.string.multiplayer_busy_game, modeLabel(it.mode), it.subjectName, it.code)
        } ?: stringResource(R.string.multiplayer_busy_unknown)
    MbDialog(
        icon = GotestIcons.Bold.InfoCircle,
        tone = MbBadgeTone.Blue,
        title = stringResource(R.string.multiplayer_busy_title),
        message = stringResource(R.string.multiplayer_busy_message, label),
        confirmText = stringResource(R.string.multiplayer_back_to_game),
        onConfirm = onResume,
        onDismissRequest = onClose,
        dismissText = stringResource(R.string.multiplayer_close),
    )
}
