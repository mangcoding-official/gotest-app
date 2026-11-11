package com.gotest.app.feature.multiplayer.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestFilterPill
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.HistoryOutcome
import com.gotest.app.feature.multiplayer.ui.components.MbGameTypeChip
import com.gotest.app.feature.multiplayer.ui.components.MbOutcomeChip
import com.gotest.app.feature.multiplayer.ui.components.MultiplayerPreviewData
import com.gotest.app.feature.multiplayer.ui.components.questionCountLabel
import java.time.YearMonth

@Composable
fun MultiplayerHistoryRoute(
    onBack: () -> Unit,
    onOpenResult: (code: String) -> Unit,
    onOpenDetail: (code: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MultiplayerHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MultiplayerHistoryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenResult = onOpenResult,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
    )
}

/** "Riwayat Main Bareng" (Lihat Semua), frame 23813. */
@Composable
fun MultiplayerHistoryScreen(
    uiState: MultiplayerHistoryUiState,
    onEvent: (MultiplayerHistoryUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenResult: (code: String) -> Unit,
    onOpenDetail: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.multiplayer_history_title),
            navigationIcon = { GotestBackButton(onClick = onBack) },
        )
        when (uiState) {
            MultiplayerHistoryUiState.Loading -> {
                GotestLoadingView()
            }

            MultiplayerHistoryUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(MultiplayerHistoryUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            MultiplayerHistoryUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.People,
                    title = stringResource(R.string.multiplayer_history_empty_title),
                    message = stringResource(R.string.multiplayer_history_empty_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is MultiplayerHistoryUiState.Success -> {
                HistoryContent(
                    state = uiState,
                    onEvent = onEvent,
                    onOpenGame = { game -> if (game.hasResult) onOpenResult(game.code) else onOpenDetail(game.code) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    state: MultiplayerHistoryUiState.Success,
    onEvent: (MultiplayerHistoryUiEvent) -> Unit,
    onOpenGame: (GameHistoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.sm,
                bottom = bottomInset + spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GameHistoryFilter.entries.forEach { filter ->
                    GotestFilterPill(
                        text = stringResource(filter.labelRes),
                        selected = filter == state.filter,
                        onClick = { onEvent(MultiplayerHistoryUiEvent.FilterSelect(filter)) },
                    )
                }
            }
        }
        item(key = "stats") {
            StatsRow(stats = state.stats)
        }
        if (state.months.isEmpty()) {
            // Only a Duel or Room filter can come up empty: no history at all is the Empty state.
            item(key = "no_games") {
                GotestEmptyView(
                    icon = GotestIcons.Linear.People,
                    title = stringResource(R.string.multiplayer_history_empty_title),
                    message =
                        stringResource(
                            R.string.multiplayer_history_filter_empty_message,
                            stringResource(state.filter.labelRes),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            state.months.forEach { month ->
                item(key = "month_${month.key}") {
                    Text(
                        text = GotestFormat.monthYear(month.month.atDay(1)),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.bodySmall,
                        color = GotestTheme.colors.textSecondary,
                    )
                }
                item(key = "games_${month.key}") {
                    GotestCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(spacing.none),
                        verticalSpacing = spacing.none,
                    ) {
                        month.games.forEach { game -> GameHistoryRow(game = game, onAction = { onOpenGame(game) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    stats: GameHistoryStats,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        StatTile(
            value = GotestFormat.number(stats.games),
            label = stringResource(R.string.multiplayer_history_stat_games),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = GotestFormat.number(stats.duelWins),
            label = stringResource(R.string.multiplayer_history_stat_duel_wins),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = GotestFormat.number(stats.roomWins),
            label = stringResource(R.string.multiplayer_history_stat_room_wins),
            modifier = Modifier.weight(1f),
        )
    }
}

/** White tile with the number over its caption. */
@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier =
            modifier
                .background(colors.bgApp, shape)
                .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, shape)
                .padding(GotestTheme.spacing.md)
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Mode and result chips, subtest, "vs Ardy Pratama • 22 Okt, 19:20" and "Lihat Hasil" or "Detail". */
@Composable
private fun GameHistoryRow(
    game: GameHistoryItem,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val action = if (game.hasResult) R.string.multiplayer_history_see_result else R.string.multiplayer_history_detail
    Row(
        modifier = modifier.fillMaxWidth().padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs)) {
                MbGameTypeChip(type = game.settings.type)
                MbOutcomeChip(outcome = game.outcome)
            }
            Text(
                text = game.settings.subtest,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = rowMeta(game),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GotestButton(
            text = stringResource(action),
            onClick = onAction,
            variant = GotestButtonVariant.Plain,
            size = GotestButtonSize.Small,
        )
    }
}

/** "vs Ardy Pratama • 22 Okt, 19:20", "6 pemain • 21 Okt, 20:05" or, without a result, "20 soal • 19 Okt, 09:12". */
@Composable
@ReadOnlyComposable
private fun rowMeta(game: GameHistoryItem): String {
    val opponent = game.opponentName
    val lead =
        when {
            !game.hasResult -> {
                questionCountLabel(game.settings.questionCount)
            }

            opponent != null -> {
                stringResource(R.string.multiplayer_versus, opponent)
            }

            game.settings.type == GameType.Room -> {
                pluralStringResource(R.plurals.multiplayer_player_count, game.playerCount, game.playerCount)
            }

            else -> {
                questionCountLabel(game.settings.questionCount)
            }
        }
    val playedAt =
        stringResource(
            R.string.multiplayer_date_time,
            GotestFormat.shortDate(game.playedAt.toLocalDate()),
            GotestFormat.time(game.playedAt),
        )
    return stringResource(R.string.multiplayer_meta_two, lead, playedAt)
}

private val previewSuccess =
    MultiplayerHistoryUiState.Success(
        filter = GameHistoryFilter.All,
        stats = GameHistoryStats(games = 14, duelWins = 6, roomWins = 3),
        months = listOf(GameHistoryMonth(YearMonth.of(2026, 10), MultiplayerPreviewData.history)),
    )

@Composable
private fun HistoryPreview(uiState: MultiplayerHistoryUiState) {
    GotestTheme {
        MultiplayerHistoryScreen(
            uiState = uiState,
            onEvent = {},
            onBack = {},
            onOpenResult = {},
            onOpenDetail = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerHistoryScreenPreview() {
    HistoryPreview(uiState = previewSuccess)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerHistoryRoomFilterPreview() {
    HistoryPreview(
        uiState =
            previewSuccess.copy(
                filter = GameHistoryFilter.Room,
                months =
                    previewSuccess.months.map { month ->
                        month.copy(games = month.games.filter { it.settings.type == GameType.Room })
                    },
            ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerHistoryNoMatchPreview() {
    HistoryPreview(uiState = previewSuccess.copy(filter = GameHistoryFilter.Duel, months = emptyList()))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerHistoryEmptyPreview() {
    HistoryPreview(uiState = MultiplayerHistoryUiState.Empty)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerHistoryLoadingPreview() {
    HistoryPreview(uiState = MultiplayerHistoryUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun MultiplayerHistoryErrorPreview() {
    HistoryPreview(uiState = MultiplayerHistoryUiState.Error)
}
