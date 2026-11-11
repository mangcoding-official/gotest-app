package com.gotest.app.feature.multiplayer.ui.waitingroom

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.ui.components.MbBadgeTone
import com.gotest.app.feature.multiplayer.ui.components.MbBottomBar
import com.gotest.app.feature.multiplayer.ui.components.MbDialog
import com.gotest.app.feature.multiplayer.ui.components.MbDialogContent
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewOverlay
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import kotlinx.coroutines.launch

private const val WHATSAPP_PACKAGE = "com.whatsapp"

/** Callbacks of the code panel's share buttons. */
@Immutable
class ShareActions(
    val onCopyCode: () -> Unit,
    val onCopyLink: () -> Unit,
    val onShareWhatsApp: () -> Unit,
)

@Composable
fun WaitingRoomRoute(
    onBack: () -> Unit,
    onGameStart: (code: String) -> Unit,
    onBackToHub: () -> Unit,
    onOpenWaitingRoom: (code: String) -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WaitingRoomViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val failedMessage = stringResource(R.string.multiplayer_action_failed)
    val codeCopied = stringResource(R.string.multiplayer_code_copied)
    val linkCopied = stringResource(R.string.multiplayer_link_copied)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is WaitingRoomEffect.GameStarted -> onGameStart(effect.code)
            WaitingRoomEffect.Left -> onBackToHub()
            is WaitingRoomEffect.Recreated -> onOpenWaitingRoom(effect.code)
            WaitingRoomEffect.UpgradeRequired -> onUpgrade()
            WaitingRoomEffect.ActionFailed -> scope.launch { snackbarHostState.showSnackbar(failedMessage) }
        }
    }
    val code = (uiState as? WaitingRoomUiState.Success)?.game?.code.orEmpty()
    val link = stringResource(R.string.multiplayer_link_url, code)
    val shareText = stringResource(R.string.multiplayer_share_text, code, link)
    val copy: (String, String) -> Unit = { text, message ->
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(code, text)))
            snackbarHostState.showSnackbar(message)
        }
    }
    WaitingRoomScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onBackToHub = onBackToHub,
        shareActions =
            ShareActions(
                onCopyCode = { copy(code, codeCopied) },
                onCopyLink = { copy(link, linkCopied) },
                onShareWhatsApp = { context.shareToWhatsApp(shareText) },
            ),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** Opens WhatsApp with the invite, or the system share sheet when WhatsApp is not installed. */
private fun Context.shareToWhatsApp(text: String) {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
    runCatching { startActivity(Intent(send).setPackage(WHATSAPP_PACKAGE)) }
        .onFailure { startActivity(Intent.createChooser(send, null)) }
}

@Composable
fun WaitingRoomScreen(
    uiState: WaitingRoomUiState,
    onEvent: (WaitingRoomUiEvent) -> Unit,
    onBack: () -> Unit,
    onBackToHub: () -> Unit,
    shareActions: ShareActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GotestTopAppBar(
                title = stringResource(R.string.multiplayer_waiting_room_title),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
            when (uiState) {
                WaitingRoomUiState.Loading -> {
                    GotestLoadingView()
                }

                WaitingRoomUiState.Error -> {
                    GotestErrorView(onRetry = { onEvent(WaitingRoomUiEvent.Retry) }, modifier = Modifier.fillMaxSize())
                }

                is WaitingRoomUiState.Success -> {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = GotestTheme.spacing.screenHorizontal,
                                    end = GotestTheme.spacing.screenHorizontal,
                                    top = GotestTheme.spacing.sm,
                                    bottom = GotestTheme.spacing.xl,
                                ),
                        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
                    ) {
                        LobbyInfoCard(state = uiState, shareActions = shareActions)
                        LobbyPlayersCard(state = uiState)
                    }
                    WaitingRoomActions(state = uiState, onEvent = onEvent)
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        (uiState as? WaitingRoomUiState.Success)?.dialog?.let { dialog ->
            WaitingRoomDialogHost(
                dialog = dialog,
                state = uiState,
                onEvent = onEvent,
                onBackToHub = onBackToHub,
            )
        }
    }
}

@Composable
private fun WaitingRoomActions(
    state: WaitingRoomUiState.Success,
    onEvent: (WaitingRoomUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val waiting = state.game.status == GameStatus.Waiting
    MbBottomBar(modifier = modifier) {
        if (state.isHost) {
            GotestButton(
                text = stringResource(R.string.multiplayer_waiting_start),
                onClick = { onEvent(WaitingRoomUiEvent.Start) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.game.canStart && !state.isBusy,
            )
            GotestButton(
                text = stringResource(R.string.multiplayer_waiting_cancel_game),
                onClick = { onEvent(WaitingRoomUiEvent.Leave) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = waiting && !state.isBusy,
            )
            Text(
                text =
                    stringResource(
                        if (state.game.type == GameType.Duel) {
                            R.string.multiplayer_waiting_start_hint_duel
                        } else {
                            R.string.multiplayer_waiting_start_hint_room
                        },
                    ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        } else {
            GotestButton(
                text = stringResource(R.string.multiplayer_waiting_leave),
                onClick = { onEvent(WaitingRoomUiEvent.Leave) },
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                enabled = waiting && !state.isBusy,
            )
        }
    }
}

@Composable
private fun WaitingRoomDialogHost(
    dialog: WaitingRoomDialog,
    state: WaitingRoomUiState.Success,
    onEvent: (WaitingRoomUiEvent) -> Unit,
    onBackToHub: () -> Unit,
) {
    when (dialog) {
        WaitingRoomDialog.CancelledByHost -> {
            MbDialog(
                icon = GotestIcons.Bold.CloseCircle,
                tone = MbBadgeTone.Red,
                title = stringResource(R.string.multiplayer_cancelled_by_host_title),
                message =
                    stringResource(
                        R.string.multiplayer_cancelled_by_host_message,
                        state.game.host
                            ?.name
                            .orEmpty(),
                    ),
                confirmText = stringResource(R.string.multiplayer_back_to_hub),
                onConfirm = onBackToHub,
                onDismissRequest = onBackToHub,
            )
        }

        WaitingRoomDialog.AutoCancelled -> {
            MbDialog(
                icon = GotestIcons.Bold.Calendar,
                tone = MbBadgeTone.Yellow,
                title = stringResource(R.string.multiplayer_auto_cancel_title),
                message = stringResource(R.string.multiplayer_auto_cancel_message),
                confirmText =
                    stringResource(
                        if (state.isHost) R.string.multiplayer_recreate else R.string.multiplayer_back_to_hub,
                    ),
                onConfirm = { if (state.isHost) onEvent(WaitingRoomUiEvent.Recreate) else onBackToHub() },
                onDismissRequest = onBackToHub,
                dismissText = if (state.isHost) stringResource(R.string.multiplayer_back) else null,
            )
        }
    }
}

private val noShare = ShareActions(onCopyCode = {}, onCopyLink = {}, onShareWhatsApp = {})

@Composable
private fun WaitingRoomPreview(state: WaitingRoomUiState) {
    WaitingRoomScreen(uiState = state, onEvent = {}, onBack = {}, onBackToHub = {}, shareActions = noShare)
}

private fun previewState(game: Game) =
    WaitingRoomUiState.Success(game = game, autoCancelSeconds = 252, nowMillis = MultiplayerPreviewData.NOW)

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomHostRoomPreview() {
    GotestTheme { WaitingRoomPreview(previewState(MultiplayerPreviewData.hostRoomLobby)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomHostDuelPreview() {
    GotestTheme { WaitingRoomPreview(previewState(MultiplayerPreviewData.hostDuelLobby)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomPlayerPreview() {
    GotestTheme { WaitingRoomPreview(previewState(MultiplayerPreviewData.playerRoomLobby)) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomLoadingPreview() {
    GotestTheme { WaitingRoomPreview(WaitingRoomUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomErrorPreview() {
    GotestTheme { WaitingRoomPreview(WaitingRoomUiState.Error) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomCancelledByHostPreview() {
    GotestTheme {
        MbPreviewOverlay(screen = { WaitingRoomPreview(previewState(MultiplayerPreviewData.playerRoomLobby)) }) {
            MbDialogContent(
                icon = GotestIcons.Bold.CloseCircle,
                tone = MbBadgeTone.Red,
                title = stringResource(R.string.multiplayer_cancelled_by_host_title),
                message = stringResource(R.string.multiplayer_cancelled_by_host_message, "Gilang Permana"),
                confirmText = stringResource(R.string.multiplayer_back_to_hub),
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun WaitingRoomAutoCancelPreview() {
    GotestTheme {
        MbPreviewOverlay(screen = { WaitingRoomPreview(previewState(MultiplayerPreviewData.hostRoomLobby)) }) {
            MbDialogContent(
                icon = GotestIcons.Bold.Calendar,
                tone = MbBadgeTone.Yellow,
                title = stringResource(R.string.multiplayer_auto_cancel_title),
                message = stringResource(R.string.multiplayer_auto_cancel_message),
                confirmText = stringResource(R.string.multiplayer_recreate),
                onConfirm = {},
                onDismiss = {},
                dismissText = stringResource(R.string.multiplayer_back),
                modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            )
        }
    }
}
