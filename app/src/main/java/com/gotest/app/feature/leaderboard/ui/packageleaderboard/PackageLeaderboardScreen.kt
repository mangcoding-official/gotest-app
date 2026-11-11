package com.gotest.app.feature.leaderboard.ui.packageleaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.ui.examChipTone
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardList
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardMetric
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardPodium

@Composable
fun PackageLeaderboardRoute(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PackageLeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PackageLeaderboardScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onSearch = onSearch,
        modifier = modifier,
    )
}

@Composable
fun PackageLeaderboardScreen(
    uiState: PackageLeaderboardUiState,
    onEvent: (PackageLeaderboardUiEvent) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val header =
        when (uiState) {
            is PackageLeaderboardUiState.Success -> uiState.header
            is PackageLeaderboardUiState.Empty -> uiState.header
            else -> null
        }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        GotestTopAppBar(
            title = stringResource(R.string.leaderboard_package_title),
            subtitle = header?.title,
            navigationIcon = { GotestBackButton(onClick = onBack) },
            actions = {
                GotestIconButton(
                    icon = GotestIcons.Linear.SearchNormal,
                    contentDescription = stringResource(R.string.leaderboard_cd_search),
                    onClick = onSearch,
                )
            },
        )
        PackageLeaderboardBody(
            uiState = uiState,
            onRetry = { onEvent(PackageLeaderboardUiEvent.Retry) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PackageLeaderboardBody(
    uiState: PackageLeaderboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val stateModifier =
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    Box(modifier = modifier) {
        when (uiState) {
            PackageLeaderboardUiState.Loading -> {
                GotestLoadingView(modifier = stateModifier)
            }

            PackageLeaderboardUiState.Error -> {
                GotestErrorView(onRetry = onRetry, modifier = stateModifier)
            }

            is PackageLeaderboardUiState.Empty -> {
                Column(modifier = stateModifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)) {
                    PackageChips(header = uiState.header)
                    GotestEmptyView(
                        icon = GotestIcons.Linear.Leaderboard,
                        title = stringResource(R.string.leaderboard_package_empty_title),
                        message = stringResource(R.string.leaderboard_package_empty_message),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is PackageLeaderboardUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = spacing.screenHorizontal,
                            end = spacing.screenHorizontal,
                            top = spacing.sm,
                            bottom = navigationBarInset + spacing.xl,
                        ),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    item(key = "chips") { PackageChips(header = uiState.header) }
                    item(key = "podium") {
                        LeaderboardPodium(
                            entries = uiState.podium,
                            metric = LeaderboardMetric.FinishTime,
                            modifier = Modifier.fillMaxWidth(),
                            shape = GotestTheme.shapes.largeIncreased,
                            contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.lg),
                        )
                    }
                    if (uiState.rows.isNotEmpty()) {
                        item(key = "rows") {
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                TableHeader()
                                LeaderboardList(
                                    entries = uiState.rows,
                                    metric = LeaderboardMetric.FinishTime,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Exam label plus "120 Soal" and "120 Menit". */
@Composable
private fun PackageChips(
    header: PackageLeaderboardHeader,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs)) {
        GotestChip(text = header.examLabel, tone = examChipTone(header.examLabel))
        GotestChip(
            text = stringResource(R.string.leaderboard_question_count, header.questionCount),
            tone = GotestChipTone.Grey,
        )
        GotestChip(
            text = stringResource(R.string.leaderboard_duration_minutes, header.durationMinutes),
            tone = GotestChipTone.Grey,
        )
    }
}

/** "Peringkat" … "Waktu • Poin" above the rows. */
@Composable
private fun TableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = GotestTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.leaderboard_table_rank),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text = stringResource(R.string.leaderboard_table_time_points),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

private val previewHeader =
    PackageLeaderboardHeader(
        title = "Tryout UTBK-SNBT 2026 (Sesi 1)",
        examLabel = "UTBK-SNBT",
        questionCount = 120,
        durationMinutes = 120,
    )

private fun previewEntry(
    rank: Int,
    name: String,
    points: Int,
    finishSeconds: Int,
    rankChange: Int = 0,
    isCurrentUser: Boolean = false,
) = LeaderboardEntry(rank, name, null, points, null, finishSeconds, rankChange, isCurrentUser)

@Composable
private fun PackageLeaderboardPreviewHost(uiState: PackageLeaderboardUiState) {
    GotestTheme {
        PackageLeaderboardScreen(
            uiState = uiState,
            onEvent = {},
            onBack = {},
            onSearch = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardSuccessPreview() {
    PackageLeaderboardPreviewHost(
        PackageLeaderboardUiState.Success(
            header = previewHeader,
            podium =
                listOf(
                    previewEntry(1, "John Stuwart", 424, 7_805),
                    previewEntry(2, "Michael T.", 324, 8_621),
                    previewEntry(3, "Emily Carter", 172, 9_612),
                ),
            rows =
                listOf(
                    previewEntry(4, "Alex Thompson", 124, 9_161, rankChange = 2),
                    previewEntry(5, "Mia Johnson", 121, 9_310, rankChange = 1),
                    previewEntry(6, "Ethan Williams", 118, 9_482, rankChange = -4),
                    previewEntry(7, "Sophia Brown", 116, 9_655),
                    previewEntry(12, "Gilang P.", 104, 10_280, rankChange = 1, isCurrentUser = true),
                ),
        ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardLoadingPreview() {
    PackageLeaderboardPreviewHost(PackageLeaderboardUiState.Loading)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardEmptyPreview() {
    PackageLeaderboardPreviewHost(PackageLeaderboardUiState.Empty(previewHeader))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PackageLeaderboardErrorPreview() {
    PackageLeaderboardPreviewHost(PackageLeaderboardUiState.Error)
}
