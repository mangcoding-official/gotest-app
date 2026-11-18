package com.gotest.app.feature.leaderboard.ui.packageboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.leaderboard.domain.LeaderboardRules
import com.gotest.app.feature.leaderboard.domain.PackageBoardEntry
import com.gotest.app.feature.leaderboard.domain.PackageBoardInfo
import com.gotest.app.feature.leaderboard.domain.PackageBoardPage
import com.gotest.app.feature.leaderboard.ui.components.BoardEmpty
import com.gotest.app.feature.leaderboard.ui.components.BoardPodium
import com.gotest.app.feature.leaderboard.ui.components.BoardTable

@Composable
fun PackageBoardRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PackageBoardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PackageBoardScreen(uiState = uiState, onBack = onBack, onRetry = viewModel::load, modifier = modifier)
}

@Composable
fun PackageBoardScreen(
    uiState: PackageBoardUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = uiState.page
    Column(modifier = modifier.fillMaxSize().background(GotestTheme.colors.bgSubtle)) {
        GotestTopAppBar(
            title = stringResource(R.string.leaderboard_title),
            navigationIcon = { GotestBackButton(onClick = onBack) },
        )
        when {
            page != null -> BoardContent(page)
            uiState.loadFailed -> GotestErrorView(onRetry = onRetry, modifier = Modifier.fillMaxSize())
            else -> GotestLoadingView()
        }
    }
}

@Composable
private fun BoardContent(
    page: PackageBoardPage,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.md,
                bottom = spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "head") { BoardHead(page.board) }
        if (page.top.isEmpty()) {
            item(key = "empty") { BoardEmpty(message = emptyMessage(page)) }
        } else {
            item(key = "podium") {
                BoardPodium(
                    top = page.top.map { it.toRow() },
                    showTime = !page.isPractice,
                    pointsLabel = page.board.pointsLabel.takeIf { page.isPractice },
                )
            }
        }
        if (page.rows.isNotEmpty()) {
            item(key = "table") {
                BoardTable(
                    rows = page.rows.map { it.toRow() },
                    pointsLabel = page.board.pointsLabel.takeIf { page.isPractice },
                )
            }
        }
    }
}

/** "Leaderboard {name}", the lede, and "{n} Soal · {time}". */
@Composable
private fun BoardHead(
    board: PackageBoardInfo,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs)) {
        Text(
            text = stringResource(R.string.leaderboard_package_title, board.name),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.leaderboard_lede),
            style = MaterialTheme.typography.bodySmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text =
                stringResource(
                    R.string.leaderboard_package_meta,
                    board.questionCount,
                    board.timeLabel ?: stringResource(R.string.leaderboard_minutes, board.durationMinutes),
                ),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

/** Web Package.tsx: Latihan, a windowed Try Out before its ranking opens ("… WIB"), or a Try Out. */
@Composable
private fun emptyMessage(page: PackageBoardPage): String {
    val opens = LeaderboardRules.instant(page.board.rankingOpensAt)
    return when {
        page.isPractice -> stringResource(R.string.leaderboard_package_empty_practice)
        opens != null -> stringResource(R.string.leaderboard_package_opens, GotestFormat.windowInstant(opens))
        else -> stringResource(R.string.leaderboard_package_empty_tryout)
    }
}

private val previewEntries =
    listOf(
        PackageBoardEntry(1, 1, "Nadia Putri", 734, durationUsed = 5_400),
        PackageBoardEntry(2, 2, "Rafi Akbar", 702, durationUsed = 5_712, change = 1),
        PackageBoardEntry(3, 3, "Salsa Aulia", 690, durationUsed = 6_002, change = -1),
        PackageBoardEntry(4, 4, "Gilang Ramadhan", 612, durationUsed = 6_400, change = 2, isCurrentUser = true),
    )

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun PackageBoardScreenPreview() {
    GotestTheme {
        PackageBoardScreen(
            uiState =
                PackageBoardUiState(
                    isLoading = false,
                    page =
                        PackageBoardPage(
                            board = PackageBoardInfo(11, "Try Out UTBK-SNBT 3", 160, 195),
                            top = previewEntries.take(3),
                            rows = previewEntries.drop(3),
                        ),
                ),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PackageBoardWindowedPreview() {
    GotestTheme {
        PackageBoardScreen(
            uiState =
                PackageBoardUiState(
                    isLoading = false,
                    page =
                        PackageBoardPage(
                            board =
                                PackageBoardInfo(
                                    12,
                                    "Try Out Bersama",
                                    160,
                                    195,
                                    rankingOpensAt = "2026-11-20T10:15:00+07:00",
                                ),
                        ),
                ),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PackageBoardErrorPreview() {
    GotestTheme {
        PackageBoardScreen(
            uiState = PackageBoardUiState(isLoading = false, loadFailed = true),
            onBack = {},
            onRetry = {},
        )
    }
}
