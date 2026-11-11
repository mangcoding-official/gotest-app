package com.gotest.app.feature.multiplayer.ui.gamewaiting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.ui.components.MbBadgeTone
import com.gotest.app.feature.multiplayer.ui.components.MbPlayerRow
import com.gotest.app.feature.multiplayer.ui.components.MbSeatList
import com.gotest.app.feature.multiplayer.ui.components.MbStatusBadge
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import com.gotest.app.feature.multiplayer.ui.components.playerName
import com.gotest.app.feature.multiplayer.ui.components.playerSubtitle
import com.gotest.app.feature.multiplayer.ui.components.rowState

@Composable
fun GameWaitingRoute(
    onBack: () -> Unit,
    onResultReady: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameWaitingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is GameWaitingEffect.ResultReady -> onResultReady(effect.code)
        }
    }
    GameWaitingScreen(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

@Composable
fun GameWaitingScreen(
    uiState: GameWaitingUiState,
    onEvent: (GameWaitingUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title = stringResource(R.string.multiplayer_title),
            navigationIcon = { GotestBackButton(onClick = onBack) },
        )
        when (uiState) {
            GameWaitingUiState.Loading -> {
                GotestLoadingView()
            }

            GameWaitingUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(GameWaitingUiEvent.Retry) }, modifier = Modifier.fillMaxSize())
            }

            is GameWaitingUiState.Success -> {
                SentCard(
                    state = uiState,
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(
                                start = GotestTheme.spacing.screenHorizontal,
                                end = GotestTheme.spacing.screenHorizontal,
                                top = GotestTheme.spacing.sm,
                                bottom = GotestTheme.spacing.xl,
                            ),
                )
            }
        }
    }
}

/** "Jawabanmu sudah terkirim" with the remaining time and who is still answering. */
@Composable
private fun SentCard(
    state: GameWaitingUiState.Success,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MbStatusBadge(icon = GotestIcons.Bold.TickCircle, tone = MbBadgeTone.Green)
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.multiplayer_sent_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.multiplayer_sent_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            RemainingTime(seconds = state.remainingSeconds)
            PlayerStatusList(state = state)
            Text(
                text = stringResource(R.string.multiplayer_sent_note),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun RemainingTime(
    seconds: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.medium)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.multiplayer_sent_time_left),
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text = GotestFormat.clock(seconds),
            style = MaterialTheme.typography.headlineMedium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun PlayerStatusList(
    state: GameWaitingUiState.Success,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.multiplayer_sent_players),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            GotestChip(
                text =
                    stringResource(
                        R.string.multiplayer_sent_finished_count,
                        state.finishedCount,
                        state.players.size,
                    ),
                tone = GotestChipTone.Green,
            )
        }
        MbSeatList {
            state.players.forEachIndexed { index, player ->
                if (index > 0) GotestDivider()
                MbPlayerRow(
                    name = playerName(player.name, player.isCurrentUser),
                    subtitle = playerSubtitle(player, state.nowMillis),
                    avatarUrl = player.avatarUrl,
                    state = player.rowState(),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameWaitingScreenPreview() {
    GotestTheme {
        GameWaitingScreen(
            uiState =
                GameWaitingUiState.Success(
                    players = MultiplayerPreviewData.playingGame.players,
                    remainingSeconds = 492,
                    nowMillis = MultiplayerPreviewData.NOW,
                ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameWaitingLoadingPreview() {
    GotestTheme { GameWaitingScreen(uiState = GameWaitingUiState.Loading, onEvent = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameWaitingErrorPreview() {
    GotestTheme { GameWaitingScreen(uiState = GameWaitingUiState.Error, onEvent = {}, onBack = {}) }
}
