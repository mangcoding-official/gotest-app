package com.gotest.app.feature.home.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.UserStats
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onContinue: (packageId: String) -> Unit,
    onBrowsePackages: () -> Unit,
    onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    onSeeAllActivity: () -> Unit,
    onOpenResult: (attemptId: String) -> Unit,
    onJoinGame: () -> Unit,
    onCreateGame: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        actions =
            HomeActions(
                onSearch = onSearch,
                onNotifications = onNotifications,
                onContinue = onContinue,
                onBrowsePackages = onBrowsePackages,
                onOpenPackage = onOpenPackage,
                onSeeAllActivity = onSeeAllActivity,
                onOpenResult = onOpenResult,
                onJoinGame = onJoinGame,
                onCreateGame = onCreateGame,
                // The promo landing page is not designed yet.
                onPromoClick = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } },
            ),
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** Navigation and other callbacks of Beranda, grouped so sections take only what they need. */
@Immutable
data class HomeActions(
    val onSearch: () -> Unit,
    val onNotifications: () -> Unit,
    val onContinue: (packageId: String) -> Unit,
    val onBrowsePackages: () -> Unit,
    val onOpenPackage: (packageId: String, type: PackageType) -> Unit,
    val onSeeAllActivity: () -> Unit,
    val onOpenResult: (attemptId: String) -> Unit,
    val onJoinGame: () -> Unit,
    val onCreateGame: () -> Unit,
    val onPromoClick: () -> Unit,
)

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        when (uiState) {
            HomeUiState.Loading -> {
                GotestLoadingView()
            }

            HomeUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(HomeUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
                )
            }

            is HomeUiState.Empty -> {
                HomeDashboard(
                    profile = uiState.profile,
                    stats = null,
                    inProgress = null,
                    isNewStudent = true,
                    recommendations = uiState.recommendations,
                    activity = HomeActivity(selectedType = uiState.activityType, attempts = emptyList()),
                    onEvent = onEvent,
                    actions = actions,
                )
            }

            is HomeUiState.Success -> {
                HomeDashboard(
                    profile = uiState.profile,
                    stats = uiState.stats,
                    inProgress = uiState.inProgress,
                    isNewStudent = false,
                    recommendations = uiState.recommendations,
                    activity = uiState.activity,
                    onEvent = onEvent,
                    actions = actions,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = GotestTheme.sizes.bottomBarHeight + bottomInset),
        )
    }
}

/** Scrolling Beranda content, drawn under the floating bottom bar. */
@Composable
private fun HomeDashboard(
    profile: HomeProfileSummary,
    stats: UserStats?,
    inProgress: StudyPackage?,
    isNewStudent: Boolean,
    recommendations: HomeRecommendations,
    activity: HomeActivity,
    onEvent: (HomeUiEvent) -> Unit,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    // The list stops at the status bar, so the page colour behind it never scrolls away.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.sm,
                bottom = GotestTheme.sizes.bottomBarHeight + bottomInset + spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "header") {
            HomeTopBar(
                profile = profile,
                onSearch = actions.onSearch,
                onNotifications = actions.onNotifications,
                modifier = Modifier.padding(bottom = spacing.sm),
            )
        }
        item(key = "promo") { HomePromoBanner(onClick = actions.onPromoClick) }
        item(key = "stats") { HomeStatsRow(stats = stats) }
        item(key = "multiplayer") {
            HomeMultiplayerCard(onJoinGame = actions.onJoinGame, onCreateGame = actions.onCreateGame)
        }
        if (isNewStudent) {
            item(key = "first_practice") { HomeFirstPracticeCard(onBrowsePackages = actions.onBrowsePackages) }
        } else if (inProgress != null) {
            item(key = "continue") {
                HomeContinueCard(studyPackage = inProgress, onContinue = { actions.onContinue(inProgress.id) })
            }
        }
        item(key = "recommended") {
            HomeRecommendedSection(
                recommendations = recommendations,
                onSelectType = { onEvent(HomeUiEvent.SelectRecommendedType(it)) },
                onSeeAll = actions.onBrowsePackages,
                onOpenPackage = actions.onOpenPackage,
            )
        }
        item(key = "activity") {
            HomeActivitySection(
                activity = activity,
                onSelectType = { onEvent(HomeUiEvent.SelectActivityType(it)) },
                onSeeAll = actions.onSeeAllActivity,
                onOpenResult = actions.onOpenResult,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val previewActions =
    HomeActions(
        onSearch = {},
        onNotifications = {},
        onContinue = {},
        onBrowsePackages = {},
        onOpenPackage = { _, _ -> },
        onSeeAllActivity = {},
        onOpenResult = {},
        onJoinGame = {},
        onCreateGame = {},
        onPromoClick = {},
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenSuccessPreview() {
    GotestTheme {
        HomeScreen(uiState = HomePreviewData.success(), onEvent = {}, actions = previewActions)
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenTryoutTabPreview() {
    GotestTheme {
        HomeScreen(
            uiState = HomePreviewData.success(recommendedType = PackageType.Tryout),
            onEvent = {},
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenEmptyPreview() {
    GotestTheme {
        HomeScreen(uiState = HomePreviewData.empty, onEvent = {}, actions = previewActions)
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenLoadingPreview() {
    GotestTheme {
        HomeScreen(uiState = HomeUiState.Loading, onEvent = {}, actions = previewActions)
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenErrorPreview() {
    GotestTheme {
        HomeScreen(uiState = HomeUiState.Error, onEvent = {}, actions = previewActions)
    }
}
