package com.gotest.app.feature.leaderboard.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestSegmentedControl
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.component.GotestTopAppBarStyle
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.model.PackageRanking
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardInfoSheet
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardList
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardMetric
import com.gotest.app.feature.leaderboard.ui.components.LeaderboardPodium
import com.gotest.app.feature.leaderboard.ui.components.MyRankCard
import com.gotest.app.feature.leaderboard.ui.components.PackageRankingCard
import java.time.LocalDateTime

@Composable
fun LeaderboardRoute(
    onOpenPackage: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LeaderboardScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenPackage = onOpenPackage,
        modifier = modifier,
    )
}

@Composable
fun LeaderboardScreen(
    uiState: LeaderboardUiState,
    onEvent: (LeaderboardUiEvent) -> Unit,
    onOpenPackage: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    // Tab roots draw under the floating bottom bar.
    val bottomInset =
        GotestTheme.sizes.bottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // One scroll position per segment, kept while switching.
    val globalListState = rememberLazyListState()
    val packagesListState = rememberLazyListState()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GotestTopAppBar(
                title = stringResource(R.string.leaderboard_title),
                subtitle = stringResource(R.string.leaderboard_subtitle),
                style = GotestTopAppBarStyle.Leading,
                actions = {
                    GotestIconButton(
                        icon = GotestIcons.Linear.InfoCircle,
                        contentDescription = stringResource(R.string.leaderboard_cd_info),
                        onClick = { onEvent(LeaderboardUiEvent.InfoClick) },
                    )
                },
            )
            GotestSegmentedControl(
                options = LeaderboardTab.entries.map { stringResource(it.labelRes) },
                selectedIndex = uiState.selectedTab.ordinal,
                onSelect = { index -> onEvent(LeaderboardUiEvent.TabSelect(LeaderboardTab.entries[index])) },
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            )
            val contentModifier = Modifier.weight(1f)
            when (uiState.selectedTab) {
                LeaderboardTab.Global -> {
                    GlobalContent(
                        state = uiState.global,
                        listState = globalListState,
                        bottomInset = bottomInset,
                        onRetry = { onEvent(LeaderboardUiEvent.RetryGlobal) },
                        modifier = contentModifier,
                    )
                }

                LeaderboardTab.PerPackage -> {
                    PackagesContent(
                        state = uiState.packages,
                        listState = packagesListState,
                        bottomInset = bottomInset,
                        onRetry = { onEvent(LeaderboardUiEvent.RetryPackages) },
                        onOpenPackage = onOpenPackage,
                        modifier = contentModifier,
                    )
                }
            }
        }
        if (uiState.isInfoVisible) {
            LeaderboardInfoSheet(onDismiss = { onEvent(LeaderboardUiEvent.InfoDismiss) })
        }
    }
}

@Composable
private fun GlobalContent(
    state: GlobalLeaderboardUiState,
    listState: LazyListState,
    bottomInset: Dp,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val stateModifier =
        Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
    Box(modifier = modifier) {
        when (state) {
            GlobalLeaderboardUiState.Loading -> {
                GotestLoadingView(modifier = stateModifier)
            }

            GlobalLeaderboardUiState.Error -> {
                GotestErrorView(onRetry = onRetry, modifier = stateModifier)
            }

            GlobalLeaderboardUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.Leaderboard,
                    title = stringResource(R.string.leaderboard_global_empty_title),
                    message = stringResource(R.string.leaderboard_global_empty_message),
                    modifier = stateModifier,
                )
            }

            is GlobalLeaderboardUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding =
                        PaddingValues(
                            start = spacing.screenHorizontal,
                            end = spacing.screenHorizontal,
                            top = spacing.sm,
                            bottom = bottomInset + spacing.xl,
                        ),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg),
                ) {
                    item(key = "my_rank") {
                        MyRankCard(
                            rank = state.myRank,
                            rankChange = state.myRankChange,
                            totalUsers = state.totalUsers,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item(key = "podium") {
                        LeaderboardPodium(
                            entries = state.podium,
                            metric = LeaderboardMetric.Attempts,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.rows.isNotEmpty()) {
                        item(key = "rows") {
                            LeaderboardList(
                                entries = state.rows,
                                metric = LeaderboardMetric.Attempts,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackagesContent(
    state: PackageRankingsUiState,
    listState: LazyListState,
    bottomInset: Dp,
    onRetry: () -> Unit,
    onOpenPackage: (packageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val stateModifier =
        Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
    Box(modifier = modifier) {
        when (state) {
            PackageRankingsUiState.Loading -> {
                GotestLoadingView(modifier = stateModifier)
            }

            PackageRankingsUiState.Error -> {
                GotestErrorView(onRetry = onRetry, modifier = stateModifier)
            }

            PackageRankingsUiState.Empty -> {
                GotestEmptyView(
                    icon = GotestIcons.Linear.TaskSquare,
                    title = stringResource(R.string.leaderboard_packages_empty_title),
                    message = stringResource(R.string.leaderboard_packages_empty_message),
                    modifier = stateModifier,
                )
            }

            is PackageRankingsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding =
                        PaddingValues(
                            start = spacing.screenHorizontal,
                            end = spacing.screenHorizontal,
                            top = spacing.sm,
                            bottom = bottomInset + spacing.xl,
                        ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    item(key = "heading") {
                        PackagesHeading(
                            updatedAt = state.updatedAt,
                            modifier = Modifier.padding(bottom = spacing.sm),
                        )
                    }
                    items(state.rankings, key = { it.packageId }) { ranking ->
                        PackageRankingCard(
                            ranking = ranking,
                            onViewLeaderboard = { onOpenPackage(ranking.packageId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** "Semua Paket Tryout" plus "… • Diperbarui 21 Okt 2026, 15:30". */
@Composable
private fun PackagesHeading(
    updatedAt: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val updated =
        stringResource(
            R.string.leaderboard_updated_at,
            GotestFormat.mediumDate(updatedAt.toLocalDate()),
            GotestFormat.time(updatedAt),
        )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs)) {
        Text(
            text = stringResource(R.string.leaderboard_packages_title),
            style = MaterialTheme.typography.titleMedium,
            color = GotestTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.leaderboard_packages_subtitle, updated),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

private fun previewEntry(
    rank: Int,
    name: String,
    points: Int,
    attempts: Int,
    rankChange: Int = 0,
    isCurrentUser: Boolean = false,
) = LeaderboardEntry(rank, name, null, points, attempts, null, rankChange, isCurrentUser)

private val previewGlobal =
    GlobalLeaderboardUiState.Success(
        myRank = 12,
        myRankChange = 1,
        totalUsers = 12_512,
        podium =
            listOf(
                previewEntry(1, "John S.", 424, 152),
                previewEntry(2, "Michael T.", 324, 148),
                previewEntry(3, "Emily C.", 172, 145),
            ),
        rows =
            listOf(
                previewEntry(4, "Alex Thompson", 124, 141, rankChange = 2),
                previewEntry(5, "Mia Johnson", 121, 138, rankChange = 1),
                previewEntry(6, "Ethan Williams", 118, 130, rankChange = -4),
                previewEntry(12, "Gilang P.", 104, 98, rankChange = 1, isCurrentUser = true),
                previewEntry(13, "Sophia Carter", 101, 96),
            ),
    )

private val previewPackages =
    PackageRankingsUiState.Success(
        rankings =
            listOf(
                PackageRanking("p1", "Tryout UTBK-SNBT 2026 (Sesi 1)", "UTBK-SNBT", 120, 120, 12, 12_412, 680, 76, 2),
                PackageRanking("p2", "Tryout UTBK-SNBT 2026 (Sesi 2)", "UTBK-SNBT", 120, 120, 7, 9_820, 712, 81, 1),
                PackageRanking("p3", "Simulasi CPNS SKD Batch 3", "CPNS", 120, 120, 40, 5_115, 402, 64, -3),
            ),
        updatedAt = LocalDateTime.of(2026, 10, 21, 15, 30),
    )

@Composable
private fun LeaderboardPreviewHost(uiState: LeaderboardUiState) {
    GotestTheme {
        LeaderboardScreen(
            uiState = uiState,
            onEvent = {},
            onOpenPackage = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LeaderboardGlobalPreview() {
    LeaderboardPreviewHost(LeaderboardUiState(global = previewGlobal, packages = previewPackages))
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LeaderboardPerPackagePreview() {
    LeaderboardPreviewHost(
        LeaderboardUiState(selectedTab = LeaderboardTab.PerPackage, global = previewGlobal, packages = previewPackages),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LeaderboardLoadingPreview() {
    LeaderboardPreviewHost(LeaderboardUiState())
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LeaderboardEmptyPreview() {
    LeaderboardPreviewHost(
        LeaderboardUiState(
            selectedTab = LeaderboardTab.PerPackage,
            global = GlobalLeaderboardUiState.Empty,
            packages = PackageRankingsUiState.Empty,
        ),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun LeaderboardErrorPreview() {
    LeaderboardPreviewHost(LeaderboardUiState(global = GlobalLeaderboardUiState.Error))
}
