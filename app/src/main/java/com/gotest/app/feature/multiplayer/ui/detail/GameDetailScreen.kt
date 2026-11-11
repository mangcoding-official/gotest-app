package com.gotest.app.feature.multiplayer.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconBadge
import com.gotest.app.core.designsystem.component.GotestIconBadgeColors
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.domain.CreateBlock
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.ui.components.MbBottomBar
import com.gotest.app.feature.multiplayer.ui.components.MbCreateBlockedDialog
import com.gotest.app.feature.multiplayer.ui.components.MbCreateBlockedDialogContent
import com.gotest.app.feature.multiplayer.ui.components.MbPreviewOverlay
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerDimens
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import com.gotest.app.feature.multiplayer.ui.components.durationLabel
import com.gotest.app.feature.multiplayer.ui.components.gameTypeLabel
import com.gotest.app.feature.multiplayer.ui.components.playedAtLabel
import com.gotest.app.feature.multiplayer.ui.components.playerName
import com.gotest.app.feature.multiplayer.ui.components.questionCountLabel
import com.gotest.app.feature.multiplayer.ui.hub.GameScreen
import java.time.LocalDateTime

@Composable
fun GameDetailRoute(
    onBack: () -> Unit,
    onRecreate: (copyFromGameCode: String) -> Unit,
    onUpgrade: () -> Unit,
    onJoinWithCode: () -> Unit,
    onOpenGame: (code: String, screen: GameScreen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is GameDetailEffect.OpenCreateGame -> onRecreate(effect.copyFromGameCode)
            GameDetailEffect.OpenUpgrade -> onUpgrade()
            GameDetailEffect.OpenJoinSheet -> onJoinWithCode()
            is GameDetailEffect.OpenGame -> onOpenGame(effect.code, effect.screen)
        }
    }
    GameDetailScreen(
        uiState = uiState,
        dialog = dialog,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

/** "Detail Permainan" of a cancelled game (frame 23999). Games with a result open the result instead. */
@Composable
fun GameDetailScreen(
    uiState: GameDetailUiState,
    dialog: CreateBlock?,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.multiplayer_game_detail_title),
            navigationIcon = { GotestBackButton(onClick = onBack) },
        )
        when (uiState) {
            GameDetailUiState.Loading -> {
                GotestLoadingView()
            }

            GameDetailUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(GameDetailUiEvent.Retry) }, modifier = Modifier.fillMaxSize())
            }

            is GameDetailUiState.Success -> {
                DetailContent(state = uiState, modifier = Modifier.weight(1f))
                MbBottomBar {
                    GotestButton(
                        text = stringResource(R.string.multiplayer_game_detail_recreate),
                        onClick = { onEvent(GameDetailUiEvent.RecreateClick) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    dialog?.let { block ->
        MbCreateBlockedDialog(
            block = block,
            onUpgrade = { onEvent(GameDetailUiEvent.UpgradeClick) },
            onJoinWithCode = { onEvent(GameDetailUiEvent.JoinWithCodeClick) },
            onReturnToGame = { onEvent(GameDetailUiEvent.ReturnToGame(it)) },
            onDismissRequest = { onEvent(GameDetailUiEvent.DismissDialog) },
        )
    }
}

@Composable
private fun DetailContent(
    state: GameDetailUiState.Success,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        StatusCard(state = state)
        InfoCard(state = state)
        Text(
            text = stringResource(R.string.multiplayer_game_detail_players),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(spacing.none),
            verticalSpacing = spacing.none,
        ) {
            state.game.players.forEach { PlayerRow(player = it) }
        }
        if (state.stockReturned) StockNote()
    }
}

/** Red cross, "Permainan dibatalkan" and why. */
@Composable
private fun StatusCard(
    state: GameDetailUiState.Success,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GotestTheme.spacing.screenHorizontal),
        verticalSpacing = GotestTheme.spacing.sm,
    ) {
        GotestIconBadge(
            icon = GotestIcons.Bold.CloseCircle,
            colors = GotestIconBadgeColors(container = colors.statusDangerSoft, content = colors.statusDanger),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            text = stringResource(R.string.multiplayer_game_detail_cancelled),
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = explanationText(state),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun explanationText(state: GameDetailUiState.Success): String =
    when (state.explanation) {
        CancelExplanation.CancelledByYou -> {
            stringResource(R.string.multiplayer_game_detail_reason_you)
        }

        CancelExplanation.CancelledByHost -> {
            stringResource(
                R.string.multiplayer_game_detail_reason_host,
                state.game.host
                    ?.name
                    .orEmpty(),
            )
        }

        CancelExplanation.TooFewPlayers -> {
            stringResource(R.string.multiplayer_game_detail_reason_too_few)
        }

        CancelExplanation.NotStarted -> {
            stringResource(R.string.multiplayer_game_detail_reason_not_started)
        }
    }

/** Mode, Subtes, Soal & durasi, Kode, Dibuat and Dibatalkan. */
@Composable
private fun InfoCard(
    state: GameDetailUiState.Success,
    modifier: Modifier = Modifier,
) {
    val settings = state.game.settings
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GotestTheme.spacing.none),
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        InfoRow(stringResource(R.string.multiplayer_game_detail_mode), gameTypeLabel(settings.type))
        InfoRow(stringResource(R.string.multiplayer_detail_subtest), settings.subtest)
        InfoRow(
            stringResource(R.string.multiplayer_game_detail_questions_duration),
            stringResource(
                R.string.multiplayer_meta_two,
                questionCountLabel(settings.questionCount),
                durationLabel(settings.durationMinutes),
            ),
        )
        InfoRow(stringResource(R.string.multiplayer_game_detail_code), state.game.code)
        InfoRow(stringResource(R.string.multiplayer_game_detail_created), playedAtLabel(state.createdAt))
        state.cancelledAt?.let { cancelledAt ->
            val time = playedAtLabel(cancelledAt)
            InfoRow(
                stringResource(R.string.multiplayer_game_detail_cancelled_at),
                if (state.isAutoCancelled) stringResource(R.string.multiplayer_game_detail_automatic, time) else time,
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(MultiplayerDimens.detailLabelWidth),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

/** A player who sat in the waiting room: "Pembuat" or "Bergabung". */
@Composable
private fun PlayerRow(
    player: GamePlayer,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val role = if (player.isHost) R.string.multiplayer_seat_host else R.string.multiplayer_game_detail_player_joined
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestAvatar(model = player.avatarUrl, contentDescription = null, size = MultiplayerDimens.detailAvatar)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = playerName(player.name, player.isCurrentUser),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(role),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun StockNote(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgApp, shape)
                .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, shape)
                .padding(GotestTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.InfoCircle),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = colors.iconDefault,
        )
        Text(
            text = stringResource(R.string.multiplayer_game_detail_stock_returned),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
    }
}

private val previewCreatedAt = LocalDateTime.of(2026, 10, 19, 9, 12)

/** Frame 23999: the student's room cancelled itself with only them in it. */
private val previewAutoCancelled =
    GameDetailUiState.Success(
        game = MultiplayerPreviewData.cancelledRoom,
        createdAt = previewCreatedAt,
        cancelledAt = previewCreatedAt.plusMinutes(5),
        isPremium = true,
        activeGame = null,
    )

@Composable
private fun DetailPreview(uiState: GameDetailUiState) {
    GotestTheme { GameDetailScreen(uiState = uiState, dialog = null, onEvent = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameDetailScreenPreview() {
    DetailPreview(uiState = previewAutoCancelled)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameDetailCancelledByHostPreview() {
    DetailPreview(
        uiState =
            previewAutoCancelled.copy(
                game = MultiplayerPreviewData.hostCancelledRoom,
                cancelledAt = previewCreatedAt.plusMinutes(3),
            ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameDetailPremiumRequiredPreview() {
    GotestTheme {
        MbPreviewOverlay(screen = { GameDetailScreen(previewAutoCancelled, null, onEvent = {}, onBack = {}) }) {
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
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameDetailLoadingPreview() {
    DetailPreview(uiState = GameDetailUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameDetailErrorPreview() {
    DetailPreview(uiState = GameDetailUiState.Error)
}
