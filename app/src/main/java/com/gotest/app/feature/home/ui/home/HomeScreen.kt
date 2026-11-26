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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.home.domain.ContinueAttempt
import com.gotest.app.feature.home.domain.DashboardAttempt
import com.gotest.app.feature.practice.ui.catalog.PracticeStartElsewhere
import com.gotest.app.feature.tryout.ui.catalog.TryoutStartElsewhere

@Composable
fun HomeRoute(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Back from a Latihan, a Try Out, or Ruang: progress, tasks, and buttons as the server has them now.
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(HomeUiEvent.Refresh)
        onPauseOrDispose { }
    }
    // The web's flash after verifying the email when the sign-up's Mitra link was dropped (DEC-154).
    val snackbar = remember { SnackbarHostState() }
    val notice = viewModel.emailVerifiedReason?.let { stringResource(R.string.email_verified_notice, it) }
    LaunchedEffect(notice) {
        // Stays until closed, as the web's flash stays on the page.
        notice?.let { snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Indefinite) }
    }
    var starting by remember { mutableStateOf<HomePackage?>(null) }
    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        actions = actions,
        onOpenPackage = { starting = it },
        modifier = modifier,
        snackbarHostState = snackbar,
    )
    HomeStartSheet(item = starting, actions = actions, onClose = { starting = null })
}

/** A "Paket Untukmu" card opens the catalog's start sheet of its mode, as the web opens its start modal. */
@Composable
private fun HomeStartSheet(
    item: HomePackage?,
    actions: HomeActions,
    onClose: () -> Unit,
) {
    when (item) {
        is HomePackage.Practice -> {
            PracticeStartElsewhere(
                packageId = item.id,
                item = item.item,
                onClose = onClose,
                onOpenRunner = actions.onOpenPracticeRunner,
                onOpenResult = actions.onOpenPracticeResult,
            )
        }

        is HomePackage.Tryout -> {
            TryoutStartElsewhere(
                packageId = item.id,
                item = item.item,
                onClose = onClose,
                onOpenRunner = actions.onOpenTryoutRunner,
                onOpenResult = actions.onOpenTryoutResult,
            )
        }

        null -> {}
    }
}

/** Where Beranda leads, grouped so sections take only what they need. */
@Immutable
data class HomeActions(
    val onSearch: () -> Unit,
    /** Null while the Notifikasi inbox is hidden (AppFeatures): no bell. */
    val onNotifications: (() -> Unit)?,
    /** "Lihat Selengkapnya" of Paket Untukmu and "Lihat Paket Latihan": the Paket tab. */
    val onBrowsePackages: () -> Unit,
    /** "Lihat Selengkapnya" of Aktivitas Terkini: Riwayat. */
    val onSeeAllActivity: () -> Unit,
    val onOpenPracticeRunner: (attemptId: Long, packageName: String) -> Unit,
    val onOpenPracticeResult: (attemptId: Long, packageName: String) -> Unit,
    val onOpenTryoutRunner: (attemptId: Long, packageName: String) -> Unit,
    val onOpenTryoutResult: (attemptId: Long) -> Unit,
    val onOpenRuang: (code: String) -> Unit,
    /** A staff member's Mitra portal, on gotest.id (A2b-2). */
    val onOpenPortal: (code: String) -> Unit,
    /** The banner's link, a gotest.id path or an https address. */
    val onOpenLink: (href: String) -> Unit,
    val onJoinGame: () -> Unit,
    val onCreateGame: () -> Unit,
    /** Null while payments are hidden (AppFeatures): no promo banner. */
    val onOpenPromo: ((promoId: String) -> Unit)?,
)

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    actions: HomeActions,
    onOpenPackage: (HomePackage) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
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

            is HomeUiState.Ready -> {
                HomeDashboard(uiState = uiState, onEvent = onEvent, actions = actions, onOpenPackage = onOpenPackage)
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars),
        )
    }
}

/** Scrolling Beranda content in the web's order (UserDashboard), drawn under the floating bottom bar. */
@Composable
private fun HomeDashboard(
    uiState: HomeUiState.Ready,
    onEvent: (HomeUiEvent) -> Unit,
    actions: HomeActions,
    onOpenPackage: (HomePackage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val page = uiState.page
    // The list stops at the status bar, so the page colour behind it never scrolls away.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding =
            PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.lg,
                bottom = GotestTheme.sizes.bottomBarHeight + bottomInset + spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "header") {
            HomeTopBar(
                profile = uiState.profile,
                onSearch = actions.onSearch,
                onNotifications = actions.onNotifications,
                modifier = Modifier.padding(bottom = spacing.sm),
            )
        }
        actions.onOpenPromo?.let { openPromo ->
            item(key = "promo") { HomePromoBanner(onClick = { openPromo(HOME_PROMO_ID) }) }
        }
        page.banner?.let { banner ->
            item(key = "banner") { HomeBanner(banner = banner, onOpen = actions.onOpenLink) }
        }
        items(page.mitraTasks, key = { "tasks-${it.organizationCode}" }) { card ->
            HomeMitraTaskCard(card = card, onOpen = { actions.onOpenRuang(card.organizationCode) })
        }
        items(page.schoolSpaces, key = { "space-${it.code}" }) { space ->
            HomeSchoolSpaceCard(
                space = space,
                onOpenRuang = { actions.onOpenRuang(space.code) },
                onOpenPortal = { actions.onOpenPortal(space.code) },
            )
        }
        item(key = "stats") { HomeStatsRow(stats = page.stats) }
        item(key = "continue") {
            HomeContinueOrStart(
                attempt = page.continueAttempt,
                hasHistory = page.stats.packagesDone > 0,
                actions = actions,
            )
        }
        item(key = "leaderboard") { HomeWeeklyLeaderboard(leaderboard = page.leaderboard) }
        item(key = "multiplayer") {
            HomeMultiplayerCard(onJoinGame = actions.onJoinGame, onCreateGame = actions.onCreateGame)
        }
        item(key = "recommended") {
            HomeRecommendedSection(
                selectedType = uiState.packageType,
                packages = uiState.packages,
                onSelectType = { onEvent(HomeUiEvent.SelectRecommendedType(it)) },
                onSeeAll = actions.onBrowsePackages,
                onOpenPackage = onOpenPackage,
            )
        }
        item(key = "activity") {
            HomeActivitySection(
                selectedType = uiState.activityType,
                attempts = uiState.activity,
                onSelectType = { onEvent(HomeUiEvent.SelectActivityType(it)) },
                onSeeAll = actions.onSeeAllActivity,
                onOpenResult = { attempt -> openResult(attempt, actions) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Web ContinueCard: the attempt in progress, or the way to the first (or next) Latihan. */
@Composable
private fun HomeContinueOrStart(
    attempt: ContinueAttempt?,
    hasHistory: Boolean,
    actions: HomeActions,
) {
    if (attempt == null) {
        HomeFirstPracticeCard(hasHistory = hasHistory, onBrowsePackages = actions.onBrowsePackages)
    } else {
        HomeContinueCard(
            attempt = attempt,
            onContinue = {
                if (attempt.isTryout) {
                    actions.onOpenTryoutRunner(attempt.id, attempt.packageName)
                } else {
                    actions.onOpenPracticeRunner(attempt.id, attempt.packageName)
                }
            },
        )
    }
}

private fun openResult(
    attempt: DashboardAttempt,
    actions: HomeActions,
) {
    if (attempt.isTryout) {
        actions.onOpenTryoutResult(attempt.id)
    } else {
        actions.onOpenPracticeResult(attempt.id, attempt.packageName)
    }
}

private val previewActions =
    HomeActions(
        onSearch = {},
        onNotifications = {},
        onBrowsePackages = {},
        onSeeAllActivity = {},
        onOpenPracticeRunner = { _, _ -> },
        onOpenPracticeResult = { _, _ -> },
        onOpenTryoutRunner = { _, _ -> },
        onOpenTryoutResult = {},
        onOpenRuang = {},
        onOpenPortal = {},
        onOpenLink = {},
        onJoinGame = {},
        onCreateGame = {},
        onOpenPromo = null,
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenReadyPreview() {
    GotestTheme {
        HomeScreen(uiState = HomePreviewData.ready(), onEvent = {}, actions = previewActions, onOpenPackage = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenNewStudentPreview() {
    GotestTheme {
        HomeScreen(
            uiState = HomePreviewData.ready(page = HomePreviewData.newStudent, packageType = PackageType.Tryout),
            onEvent = {},
            actions = previewActions,
            onOpenPackage = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenLoadingPreview() {
    GotestTheme {
        HomeScreen(uiState = HomeUiState.Loading, onEvent = {}, actions = previewActions, onOpenPackage = {})
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun HomeScreenErrorPreview() {
    GotestTheme {
        HomeScreen(uiState = HomeUiState.Error, onEvent = {}, actions = previewActions, onOpenPackage = {})
    }
}
