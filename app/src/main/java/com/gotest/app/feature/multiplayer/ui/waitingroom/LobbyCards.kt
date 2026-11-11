package com.gotest.app.feature.multiplayer.ui.waitingroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.ui.components.DotChipTone
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbCodeBoxes
import com.gotest.app.feature.multiplayer.ui.components.MbDotChip
import com.gotest.app.feature.multiplayer.ui.components.MbInfoTile
import com.gotest.app.feature.multiplayer.ui.components.MbPlayerRow
import com.gotest.app.feature.multiplayer.ui.components.MbSeatList
import com.gotest.app.feature.multiplayer.ui.components.PlayerRowState
import com.gotest.app.feature.multiplayer.ui.components.countOfLabel
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.gameTitle
import com.gotest.app.feature.multiplayer.ui.components.gameTypeLabel
import com.gotest.app.feature.multiplayer.ui.components.playerName
import com.gotest.app.feature.multiplayer.ui.components.playerSubtitle
import com.gotest.app.feature.multiplayer.ui.components.questionCountLabel
import com.gotest.app.feature.multiplayer.ui.components.rowState

/** Status, title, game code with share buttons, details and the auto-cancel countdown. */
@Composable
fun LobbyInfoCard(
    state: WaitingRoomUiState.Success,
    shareActions: ShareActions,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.lg) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MbDotChip(text = stringResource(R.string.multiplayer_status_waiting), tone = DotChipTone.Yellow)
            Text(
                text = gameTitle(game.type, game.settings.subtest),
                style = MaterialTheme.typography.headlineMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.multiplayer_waiting_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            CodePanel(code = game.code, shareActions = shareActions)
            // Duel puts the countdown first (frame 15103); rooms show it after the details.
            if (game.type == GameType.Duel) AutoCancelBar(seconds = state.autoCancelSeconds)
            LobbyDetails(state = state)
            if (!state.isHost) {
                MbAlert(
                    type = MbAlertType.Info,
                    title = stringResource(R.string.multiplayer_waiting_for_host_title),
                    message = stringResource(R.string.multiplayer_waiting_for_host_message),
                )
            }
            if (game.type == GameType.Room) AutoCancelBar(seconds = state.autoCancelSeconds)
        }
    }
}

@Composable
private fun CodePanel(
    code: String,
    shareActions: ShareActions,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.medium)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_waiting_code_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.multiplayer_link_display, code),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        MbCodeBoxes(code = code, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ShareButton(stringResource(R.string.multiplayer_copy_code), shareActions.onCopyCode, Modifier.weight(1f))
            ShareButton(stringResource(R.string.multiplayer_copy_link), shareActions.onCopyLink, Modifier.weight(1f))
            ShareButton(
                stringResource(R.string.multiplayer_share_whatsapp),
                shareActions.onShareWhatsApp,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ShareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = GotestButtonVariant.Plain,
        size = GotestButtonSize.Medium,
    )
}

@Composable
private fun LobbyDetails(
    state: WaitingRoomUiState.Success,
    modifier: Modifier = Modifier,
) {
    val settings = state.game.settings
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_type),
                value = gameTypeLabel(settings.type),
                modifier = Modifier.weight(1f),
                filled = false,
            )
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_subtest),
                value = settings.subtest,
                modifier = Modifier.weight(1f),
                filled = false,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_question_count),
                value = questionCountLabel(settings.questionCount),
                modifier = Modifier.weight(1f),
                filled = false,
            )
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_duration),
                value = durationLabel(settings.durationMinutes),
                modifier = Modifier.weight(1f),
                filled = false,
            )
        }
    }
}

/** Yellow "Batal otomatis dalam 04:12" bar. */
@Composable
private fun AutoCancelBar(
    seconds: Int,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val clock = GotestFormat.clock(seconds)
    val description = stringResource(R.string.multiplayer_cd_auto_cancel, clock)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.illustrationYellowSoft, MaterialTheme.shapes.small)
                .padding(horizontal = GotestTheme.spacing.lg, vertical = GotestTheme.spacing.sm)
                .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.multiplayer_auto_cancel_in),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = colors.illustrationYellowDark,
        )
        Text(text = clock, style = MaterialTheme.typography.headlineSmall, color = colors.illustrationYellowDark)
    }
}

/** "Pemain 3 dari 10" with every seat, free seats included. */
@Composable
fun LobbyPlayersCard(
    state: WaitingRoomUiState.Success,
    modifier: Modifier = Modifier,
) {
    val game = state.game
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = GotestTheme.spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_detail_players),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            GotestChip(
                text = countOfLabel(game.players.size, game.maxPlayers),
                tone = GotestChipTone.Violet,
                leadingIcon = GotestIcons.Bold.People,
            )
        }
        MbSeatList {
            game.players.forEachIndexed { index, player ->
                if (index > 0) GotestDivider()
                LobbySeat(player = player, nowMillis = state.nowMillis)
            }
            if (game.seatsLeft > 0) {
                GotestDivider()
                MbPlayerRow(
                    name = stringResource(R.string.multiplayer_seat_empty),
                    subtitle =
                        if (game.type == GameType.Duel) {
                            stringResource(R.string.multiplayer_seat_empty_duel)
                        } else {
                            pluralStringResource(R.plurals.multiplayer_seats_left, game.seatsLeft, game.seatsLeft)
                        },
                    avatarUrl = null,
                    state = PlayerRowState.Empty,
                )
            }
        }
    }
}

@Composable
private fun LobbySeat(
    player: GamePlayer,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    MbPlayerRow(
        name = playerName(player.name, player.isCurrentUser),
        subtitle = playerSubtitle(player, nowMillis),
        avatarUrl = player.avatarUrl,
        state = player.rowState(),
        modifier = modifier,
    )
}
