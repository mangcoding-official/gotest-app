package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.CreateBlock
import com.gotest.app.feature.multiplayer.domain.Game

/**
 * Why a game cannot be created yet: "Buat permainan khusus Premium" (frame 13995) or "Kamu sedang
 * ikut permainan lain" (frame 14439). Shown by every entry point to "Buat permainan".
 */
@Composable
fun MbCreateBlockedDialog(
    block: CreateBlock,
    onUpgrade: () -> Unit,
    onJoinWithCode: () -> Unit,
    onReturnToGame: (Game) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        MbCreateBlockedDialogContent(
            block = block,
            onUpgrade = onUpgrade,
            onJoinWithCode = onJoinWithCode,
            onReturnToGame = onReturnToGame,
            onClose = onDismissRequest,
            modifier = modifier,
        )
    }
}

/** The dialog card without its window, for previews. */
@Composable
fun MbCreateBlockedDialogContent(
    block: CreateBlock,
    onUpgrade: () -> Unit,
    onJoinWithCode: () -> Unit,
    onReturnToGame: (Game) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (block) {
        CreateBlock.PremiumRequired -> {
            MbDialogContent(
                icon = GotestIcons.Bold.Award,
                tone = MbBadgeTone.Yellow,
                title = stringResource(R.string.multiplayer_premium_dialog_title),
                message = stringResource(R.string.multiplayer_premium_dialog_message),
                confirmText = stringResource(R.string.multiplayer_premium_dialog_upgrade),
                onConfirm = onUpgrade,
                onDismiss = onJoinWithCode,
                modifier = modifier,
                dismissText = stringResource(R.string.multiplayer_join_title),
            )
        }

        is CreateBlock.InAnotherGame -> {
            MbDialogContent(
                icon = GotestIcons.Bold.InfoCircle,
                tone = MbBadgeTone.Blue,
                title = stringResource(R.string.multiplayer_in_game_title),
                message =
                    stringResource(
                        R.string.multiplayer_in_game_create_message,
                        gameTitle(block.game.type, block.game.settings.subtest),
                        block.game.code,
                    ),
                confirmText = stringResource(R.string.multiplayer_back_to_game),
                onConfirm = { onReturnToGame(block.game) },
                onDismiss = onClose,
                modifier = modifier,
                dismissText = stringResource(R.string.multiplayer_close),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MbCreateBlockedDialogPremiumPreview() {
    GotestTheme {
        MbCreateBlockedDialogContent(
            block = CreateBlock.PremiumRequired,
            onUpgrade = {},
            onJoinWithCode = {},
            onReturnToGame = {},
            onClose = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MbCreateBlockedDialogInGamePreview() {
    GotestTheme {
        MbCreateBlockedDialogContent(
            block = CreateBlock.InAnotherGame(MultiplayerPreviewData.hostDuelLobby),
            onUpgrade = {},
            onJoinWithCode = {},
            onReturnToGame = {},
            onClose = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}
