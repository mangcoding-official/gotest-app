package com.gotest.app.feature.multiplayer.ui.lobby

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbBadgeTone
import com.gotest.app.feature.multiplayer.ui.components.MbCodeBoxes
import com.gotest.app.feature.multiplayer.ui.components.MbDialog
import com.gotest.app.feature.multiplayer.ui.components.MbInfoTile
import com.gotest.app.feature.multiplayer.ui.components.MbPlayerRow
import com.gotest.app.feature.multiplayer.ui.components.MbSeatList
import com.gotest.app.feature.multiplayer.ui.components.PlayerRowState
import com.gotest.app.feature.multiplayer.ui.components.formatClock
import com.gotest.app.feature.multiplayer.ui.components.gameTitle
import com.gotest.app.feature.multiplayer.ui.components.modeLabel
import com.gotest.app.feature.multiplayer.ui.components.playerName
import com.gotest.app.feature.multiplayer.ui.components.problemText
import com.gotest.app.feature.multiplayer.ui.components.timeAgo
import com.gotest.app.feature.multiplayer.ui.hub.Body
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the waiting room can do; the confirmations live on the screen. */
internal data class LobbyActions(
    val onStart: () -> Unit,
    val onCancel: () -> Unit,
    val onLeave: () -> Unit,
    val onBack: () -> Unit,
    val onCreateAgain: () -> Unit,
)

private enum class LobbyConfirm { None, Cancel, Leave }

/** The waiting room for the host and for players, duel and room (web Lobby.tsx). */
@Composable
internal fun LobbyContent(
    game: GameState,
    secondsLeft: Long?,
    busy: Boolean,
    error: GameProblem?,
    actions: LobbyActions,
    modifier: Modifier = Modifier,
) {
    var confirm by rememberSaveable { mutableStateOf(LobbyConfirm.None) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        GameCard(game = game, secondsLeft = secondsLeft ?: 0)
        PlayersCard(game = game, busy = busy, error = error, onConfirm = { confirm = it }, onStart = actions.onStart)
    }
    when (confirm) {
        LobbyConfirm.Cancel -> {
            MbDialog(
                icon = GotestIcons.Bold.CloseCircle,
                tone = MbBadgeTone.Red,
                title = stringResource(R.string.multiplayer_confirm_cancel_title),
                message = stringResource(R.string.multiplayer_confirm_cancel_message),
                confirmText = stringResource(R.string.multiplayer_confirm_cancel),
                onConfirm = {
                    confirm = LobbyConfirm.None
                    actions.onCancel()
                },
                onDismissRequest = { confirm = LobbyConfirm.None },
                dismissText = stringResource(R.string.multiplayer_back),
            )
        }

        LobbyConfirm.Leave -> {
            MbDialog(
                icon = GotestIcons.Bold.InfoCircle,
                tone = MbBadgeTone.Yellow,
                title = stringResource(R.string.multiplayer_confirm_leave_title),
                message = stringResource(R.string.multiplayer_confirm_leave_message),
                confirmText = stringResource(R.string.multiplayer_confirm_leave),
                onConfirm = {
                    confirm = LobbyConfirm.None
                    actions.onLeave()
                },
                onDismissRequest = { confirm = LobbyConfirm.None },
                dismissText = stringResource(R.string.multiplayer_back),
            )
        }

        LobbyConfirm.None -> {}
    }
    if (game.cancelled) CancelledDialog(game = game, onBack = actions.onBack, onCreateAgain = actions.onCreateAgain)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GameCard(
    game: GameState,
    secondsLeft: Long,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            GotestChip(stringResource(R.string.multiplayer_status_waiting), tone = GotestChipTone.Yellow)
            game.organization?.let {
                GotestChip(stringResource(R.string.multiplayer_space, it.name), tone = GotestChipTone.Violet)
            }
        }
        Text(
            text = gameTitle(game.mode, game.subjectName),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
        Body(stringResource(R.string.multiplayer_waiting_subtitle))
        CodePanel(game)
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_type),
                value = modeLabel(game.mode),
                modifier = Modifier.weight(1f),
                filled = false,
            )
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_subtest),
                value = game.subjectName,
                modifier = Modifier.weight(1f),
                filled = false,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_question_count),
                value = stringResource(R.string.multiplayer_questions, game.questionCount),
                modifier = Modifier.weight(1f),
                filled = false,
            )
            MbInfoTile(
                label = stringResource(R.string.multiplayer_detail_duration),
                value = stringResource(R.string.multiplayer_minutes, game.durationMinutes),
                modifier = Modifier.weight(1f),
                filled = false,
            )
        }
        if (!game.youAreHost) {
            MbAlert(
                type = MbAlertType.Info,
                title = stringResource(R.string.multiplayer_waiting_for_host_title),
                message = stringResource(R.string.multiplayer_waiting_for_host_message),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgYellowSoft, MaterialTheme.shapes.medium)
                    .padding(horizontal = GotestTheme.spacing.lg, vertical = GotestTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.multiplayer_auto_cancel_in),
                style = MaterialTheme.typography.bodySmall,
                color = colors.bgYellowDark,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatClock(secondsLeft),
                style = MaterialTheme.typography.titleLarge,
                color = colors.bgYellowDark,
            )
        }
    }
}

/** The code, "Salin kode", "Salin tautan", and WhatsApp with the web's invitation. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodePanel(game: GameState) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val link = stringResource(R.string.multiplayer_link_url, game.code)
    val invite =
        stringResource(
            R.string.multiplayer_share_text,
            game.subjectName,
            game.questionCount,
            game.durationMinutes,
            game.code,
            link,
        )
    GotestCard(modifier = Modifier.fillMaxWidth(), containerColor = GotestTheme.colors.bgSubtle, showBorder = false) {
        Text(
            text = stringResource(R.string.multiplayer_waiting_code_label),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        Body(link.removePrefix("https://"))
        MbCodeBoxes(code = game.code)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            CopyButton(label = stringResource(R.string.multiplayer_copy_code)) {
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(game.code, game.code))) }
            }
            CopyButton(label = stringResource(R.string.multiplayer_copy_link)) {
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(game.code, link))) }
            }
            GotestButton(
                text = stringResource(R.string.multiplayer_share_whatsapp),
                onClick = { context.shareToWhatsApp(invite) },
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Small,
            )
        }
    }
}

/** "Salin kode" turns into "Tersalin" for a moment (web CopyButton). */
@Composable
private fun CopyButton(
    label: String,
    onCopy: () -> Unit,
) {
    var copied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    GotestButton(
        text = if (copied) stringResource(R.string.multiplayer_copied) else label,
        onClick = {
            onCopy()
            copied = true
        },
        variant = GotestButtonVariant.Plain,
        size = GotestButtonSize.Small,
    )
}

@Composable
private fun PlayersCard(
    game: GameState,
    busy: Boolean,
    error: GameProblem?,
    onConfirm: (LobbyConfirm) -> Unit,
    onStart: () -> Unit,
) {
    val joined = game.players.size
    val openSeats = (game.maxPlayers - joined).coerceAtLeast(0)
    GotestCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_players),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            GotestChip(
                stringResource(R.string.multiplayer_count_of, joined, game.maxPlayers),
                tone = GotestChipTone.Violet,
            )
        }
        MbSeatList {
            game.players.forEach { player ->
                MbPlayerRow(
                    name = playerName(player.name, player.isYou),
                    subtitle =
                        if (player.isHost) {
                            stringResource(R.string.multiplayer_seat_host_subtitle)
                        } else {
                            stringResource(R.string.multiplayer_joined_ago, timeAgo(player.joinedAt, game.serverNow))
                        },
                    state = if (player.isHost) PlayerRowState.Host else PlayerRowState.Joined,
                )
            }
            if (openSeats > 0) {
                MbPlayerRow(
                    name = stringResource(R.string.multiplayer_seat_empty),
                    subtitle =
                        if (game.isDuel) {
                            stringResource(R.string.multiplayer_seat_empty_duel)
                        } else {
                            stringResource(R.string.multiplayer_seats_left, openSeats)
                        },
                    state = PlayerRowState.Empty,
                )
            }
        }
        error?.let {
            MbAlert(
                type = MbAlertType.Error,
                title = stringResource(R.string.multiplayer_error_title),
                message = problemText(it),
            )
        }
        LobbyButtons(game = game, busy = busy, onConfirm = onConfirm, onStart = onStart)
    }
}

/** The host starts or cancels; a player leaves (web Lobby buttons). */
@Composable
private fun LobbyButtons(
    game: GameState,
    busy: Boolean,
    onConfirm: (LobbyConfirm) -> Unit,
    onStart: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        if (game.youAreHost) {
            GotestButton(
                text =
                    stringResource(
                        if (busy) R.string.multiplayer_waiting_starting else R.string.multiplayer_waiting_start,
                    ),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = game.canStart && !busy && !game.cancelled,
            )
            GotestButton(
                text = stringResource(R.string.multiplayer_waiting_cancel_game),
                onClick = { onConfirm(LobbyConfirm.Cancel) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = !busy && !game.cancelled,
            )
            val hint =
                if (game.isDuel) {
                    R.string.multiplayer_waiting_start_hint_duel
                } else {
                    R.string.multiplayer_waiting_start_hint_room
                }
            Body(stringResource(hint))
        } else {
            GotestButton(
                text = stringResource(R.string.multiplayer_waiting_leave),
                onClick = { onConfirm(LobbyConfirm.Leave) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = !busy && !game.cancelled,
            )
        }
    }
}

/** The game was cancelled: by the host, by the waiting room expiring, or because the host left the Mitra. */
@Composable
private fun CancelledDialog(
    game: GameState,
    onBack: () -> Unit,
    onCreateAgain: () -> Unit,
) {
    val expired = game.cancelReason == GameState.CANCEL_EXPIRED
    val message = cancelledMessage(game)
    val title =
        when {
            expired -> R.string.multiplayer_auto_cancel_title
            game.youAreHost || game.cancelReason == GameState.CANCEL_REMOVED -> R.string.multiplayer_cancelled_title
            else -> R.string.multiplayer_cancelled_by_host_title
        }
    val hostRecreates = expired && game.youAreHost
    MbDialog(
        icon = if (expired) GotestIcons.Bold.Calendar else GotestIcons.Bold.CloseCircle,
        tone = if (expired) MbBadgeTone.Yellow else MbBadgeTone.Red,
        title = stringResource(title),
        message = message,
        confirmText =
            stringResource(
                if (hostRecreates) R.string.multiplayer_recreate else R.string.multiplayer_back_to_hub,
            ),
        onConfirm = if (hostRecreates) onCreateAgain else onBack,
        // Not closeable: the only ways out are the buttons (web `closeable={false}`).
        onDismissRequest = {},
        dismissText = if (hostRecreates) stringResource(R.string.multiplayer_back) else null,
        onDismiss = onBack,
    )
}

@Composable
private fun cancelledMessage(game: GameState): String =
    when {
        game.cancelReason == GameState.CANCEL_EXPIRED -> {
            val base = stringResource(R.string.multiplayer_auto_cancel_message, GameRules.LOBBY_MINUTES)
            if (game.youAreHost) base + " " + stringResource(R.string.multiplayer_auto_cancel_host) else base
        }

        game.cancelReason == GameState.CANCEL_REMOVED -> {
            stringResource(
                R.string.multiplayer_cancelled_removed,
                game.organization?.name ?: stringResource(R.string.multiplayer_cancelled_mitra),
            )
        }

        game.youAreHost -> {
            stringResource(R.string.multiplayer_cancelled_you)
        }

        else -> {
            stringResource(R.string.multiplayer_cancelled_by_host_message, game.hostName)
        }
    }

/** Opens WhatsApp with the invitation, or the system share sheet when WhatsApp is not installed. */
private fun Context.shareToWhatsApp(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { startActivity(Intent(send).setPackage(WHATSAPP_PACKAGE)) }
        .onFailure { startActivity(Intent.createChooser(send, null)) }
}

private const val WHATSAPP_PACKAGE = "com.whatsapp"
private const val COPIED_MS = 1_800L
