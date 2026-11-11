package com.gotest.app.feature.multiplayer.ui.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.ui.components.DotChipTone
import com.gotest.app.feature.multiplayer.ui.components.MbAlert
import com.gotest.app.feature.multiplayer.ui.components.MbAlertType
import com.gotest.app.feature.multiplayer.ui.components.MbCodeInput
import com.gotest.app.feature.multiplayer.ui.components.MbDotChip
import com.gotest.app.feature.multiplayer.ui.components.MbIconTile
import com.gotest.app.feature.multiplayer.ui.components.MbInfoTile
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewSheet
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerDimens
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import com.gotest.app.feature.multiplayer.ui.components.countOfLabel
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.gameTitle
import com.gotest.app.feature.multiplayer.ui.components.questionCountLabel

/** "Gabung dengan kode" bottom sheet (frames 16162 to 18550). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameSheet(
    state: JoinSheetState,
    onEvent: (MultiplayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    GotestBottomSheet(
        onDismissRequest = { onEvent(MultiplayerUiEvent.DismissJoinSheet) },
        modifier = modifier,
    ) {
        JoinGameSheetContent(state = state, onEvent = onEvent, focusRequester = focusRequester)
        // Runs once the sheet's content is attached, so the code field can take focus.
        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    }
}

@Composable
fun JoinGameSheetContent(
    state: JoinSheetState,
    onEvent: (MultiplayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val status = state.status
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        MbIconTile(
            icon = GotestIcons.Bold.Link,
            container = colors.bgBlueSoft,
            tint = colors.bgBlue,
            size = MultiplayerDimens.sheetTile,
            iconSize = GotestTheme.sizes.iconLg,
            modifier = Modifier.padding(top = spacing.sm),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.multiplayer_join_sheet_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.multiplayer_join_sheet_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        MbCodeInput(
            value = state.code,
            onValueChange = { onEvent(MultiplayerUiEvent.CodeChange(it)) },
            onDone = { onEvent(MultiplayerUiEvent.SearchGame) },
            isError = status == JoinStatus.NotFound,
            focusRequester = focusRequester,
        )
        JoinStatusMessage(status = status)
        JoinActions(state = state, onEvent = onEvent)
        if (status == JoinStatus.Idle || status == JoinStatus.Searching || status is JoinStatus.Found) {
            Text(
                text = stringResource(R.string.multiplayer_join_sheet_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun JoinStatusMessage(
    status: JoinStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        // Nothing to show until a lookup finishes.
        JoinStatus.Idle, JoinStatus.Searching -> {}

        is JoinStatus.Found -> {
            FoundGameCard(game = status.game, modifier = modifier)
        }

        JoinStatus.NotFound -> {
            MbAlert(
                type = MbAlertType.Error,
                title = stringResource(R.string.multiplayer_join_not_found_title),
                message = stringResource(R.string.multiplayer_join_not_found_message),
                modifier = modifier,
            )
        }

        JoinStatus.AlreadyStarted -> {
            MbAlert(
                type = MbAlertType.Warning,
                title = stringResource(R.string.multiplayer_join_started_title),
                message = stringResource(R.string.multiplayer_join_started_message),
                modifier = modifier,
            )
        }

        JoinStatus.Full -> {
            MbAlert(
                type = MbAlertType.Warning,
                title = stringResource(R.string.multiplayer_join_full_title),
                message = stringResource(R.string.multiplayer_join_full_message),
                modifier = modifier,
            )
        }

        JoinStatus.Inactive -> {
            MbAlert(
                type = MbAlertType.Error,
                title = stringResource(R.string.multiplayer_join_inactive_title),
                message = stringResource(R.string.multiplayer_join_inactive_message),
                modifier = modifier,
            )
        }

        is JoinStatus.InAnotherGame -> {
            MbAlert(
                type = MbAlertType.Info,
                title = stringResource(R.string.multiplayer_in_game_title),
                message =
                    stringResource(
                        R.string.multiplayer_in_game_join_message,
                        gameTitle(status.game.type, status.game.settings.subtest),
                        status.game.code,
                    ),
                modifier = modifier,
            )
        }
    }
}

/** "Batal" plus the action that fits the lookup result. */
@Composable
private fun JoinActions(
    state: JoinSheetState,
    onEvent: (MultiplayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.status
    val (label, enabled, event) =
        when (status) {
            is JoinStatus.Found -> {
                Triple(R.string.multiplayer_join_action, !status.isJoining, MultiplayerUiEvent.JoinGame)
            }

            JoinStatus.AlreadyStarted, JoinStatus.Full, JoinStatus.Inactive -> {
                Triple(R.string.multiplayer_back_to_hub, true, MultiplayerUiEvent.DismissJoinSheet)
            }

            is JoinStatus.InAnotherGame -> {
                Triple(R.string.multiplayer_back_to_game, true, MultiplayerUiEvent.ReturnToGame(status.game))
            }

            JoinStatus.Idle, JoinStatus.Searching, JoinStatus.NotFound -> {
                Triple(
                    R.string.multiplayer_join_search,
                    state.isComplete && status != JoinStatus.Searching,
                    MultiplayerUiEvent.SearchGame,
                )
            }
        }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        GotestButton(
            text = stringResource(R.string.multiplayer_cancel),
            onClick = { onEvent(MultiplayerUiEvent.DismissJoinSheet) },
            modifier = Modifier.weight(1f),
            variant = GotestButtonVariant.Plain,
        )
        GotestButton(
            text = stringResource(label),
            onClick = { onEvent(event) },
            modifier = Modifier.weight(1f),
            enabled = enabled,
        )
    }
}

/** Preview of the lobby behind a valid code. */
@Composable
private fun FoundGameCard(
    game: Game,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = gameTitle(game.type, game.settings.subtest),
                    modifier = Modifier.weight(1f).padding(end = spacing.sm),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MbDotChip(text = stringResource(R.string.multiplayer_status_waiting), tone = DotChipTone.Yellow)
            }
            game.host?.let { host ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GotestAvatar(model = host.avatarUrl, contentDescription = null, size = GotestTheme.sizes.avatarXs)
                    Text(
                        text = stringResource(R.string.multiplayer_created_by, host.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_question_count),
                    value = questionCountLabel(game.settings.questionCount),
                    modifier = Modifier.weight(1f),
                )
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_duration),
                    value = durationLabel(game.settings.durationMinutes),
                    modifier = Modifier.weight(1f),
                )
                MbInfoTile(
                    label = stringResource(R.string.multiplayer_detail_players),
                    value = countOfLabel(game.players.size, game.maxPlayers),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinGameSheetFoundPreview() {
    GotestTheme {
        MbPreviewSheet {
            JoinGameSheetContent(
                state = JoinSheetState(code = "KTR4QZ", status = JoinStatus.Found(MultiplayerPreviewData.roomLobby)),
                onEvent = {},
            )
        }
    }
}
