package com.gotest.app.feature.multiplayer.ui.result

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.multiplayer.domain.DuelOutcome
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.ui.components.MbBottomBar
import com.gotest.app.feature.multiplayer.ui.components.MbDuelResultBanner
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import kotlinx.coroutines.launch

@Composable
fun GameResultRoute(
    onBackToHub: () -> Unit,
    onUpgrade: () -> Unit,
    onReplay: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val failedMessage = stringResource(R.string.multiplayer_create_failed)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            GameResultEffect.UpgradeRequired -> onUpgrade()
            is GameResultEffect.Replayed -> onReplay(effect.code)
            GameResultEffect.Failed -> scope.launch { snackbarHostState.showSnackbar(failedMessage) }
        }
    }
    GameResultScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackToHub = onBackToHub,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun GameResultScreen(
    uiState: GameResultUiState,
    onEvent: (GameResultUiEvent) -> Unit,
    onBackToHub: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title =
        when ((uiState as? GameResultUiState.Success)?.type) {
            GameType.Room -> stringResource(R.string.multiplayer_result_title_room)
            GameType.Duel -> stringResource(R.string.multiplayer_result_title_duel)
            null -> stringResource(R.string.multiplayer_title)
        }
    Box(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        Column(modifier = Modifier.fillMaxSize()) {
            GotestTopAppBar(title = title, navigationIcon = { GotestBackButton(onClick = onBackToHub) })
            when (uiState) {
                GameResultUiState.Loading -> {
                    GotestLoadingView()
                }

                GameResultUiState.Error -> {
                    GotestErrorView(onRetry = { onEvent(GameResultUiEvent.Retry) }, modifier = Modifier.fillMaxSize())
                }

                is GameResultUiState.Success -> {
                    ResultContent(state = uiState, modifier = Modifier.weight(1f))
                    MbBottomBar {
                        Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
                            GotestButton(
                                text = stringResource(R.string.multiplayer_back_to_hub),
                                onClick = onBackToHub,
                                modifier = Modifier.weight(1f),
                                variant = GotestButtonVariant.Plain,
                            )
                            GotestButton(
                                text = stringResource(R.string.multiplayer_result_play_again),
                                onClick = { onEvent(GameResultUiEvent.PlayAgain) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isReplaying,
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ResultContent(
    state: GameResultUiState.Success,
    modifier: Modifier = Modifier,
) {
    val result = state.result
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        ResultTitleCard(result = result)
        val outcome = state.duelOutcome
        val me = result.me
        val opponent = result.standings.firstOrNull { !it.isCurrentUser }
        // duelOutcome is only set for duels.
        if (outcome != null && me != null && opponent != null) {
            MbDuelResultBanner(outcome = outcome)
            VersusCard(
                me = me,
                opponent = opponent,
                winnerId = if (outcome == DuelOutcome.Draw) null else result.winner?.playerId,
            )
        } else {
            result.winner?.let { ChampionCard(champion = it) }
            RankingCard(standings = result.standings)
        }
        MySummaryCard(result = result)
        Text(
            text = stringResource(R.string.multiplayer_result_footer),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameResultRoomPreview() {
    GotestTheme {
        GameResultScreen(
            uiState = GameResultUiState.Success(MultiplayerPreviewData.roomResult),
            onEvent = {},
            onBackToHub = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameResultDuelWinPreview() {
    GotestTheme {
        GameResultScreen(
            uiState = GameResultUiState.Success(MultiplayerPreviewData.duelResult),
            onEvent = {},
            onBackToHub = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameResultDuelLosePreview() {
    val result = MultiplayerPreviewData.duelResult
    val lost =
        result.copy(
            standings =
                result.standings
                    .map { if (it.isCurrentUser) it.copy(rank = 2, correct = 12) else it.copy(rank = 1) }
                    .sortedBy { it.rank },
        )
    GotestTheme {
        GameResultScreen(uiState = GameResultUiState.Success(lost), onEvent = {}, onBackToHub = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameResultDuelDrawPreview() {
    val result = MultiplayerPreviewData.duelResult
    val draw = result.copy(standings = result.standings.map { it.copy(correct = 16, totalSeconds = 782) })
    GotestTheme {
        GameResultScreen(uiState = GameResultUiState.Success(draw), onEvent = {}, onBackToHub = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameResultLoadingPreview() {
    GotestTheme { GameResultScreen(uiState = GameResultUiState.Loading, onEvent = {}, onBackToHub = {}) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun GameResultErrorPreview() {
    GotestTheme { GameResultScreen(uiState = GameResultUiState.Error, onEvent = {}, onBackToHub = {}) }
}
