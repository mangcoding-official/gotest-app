package com.gotest.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gotest.app.R
import com.gotest.app.core.config.AppFeatures
import com.gotest.app.core.designsystem.component.GotestBottomBar
import com.gotest.app.core.designsystem.component.GotestBottomBarItem
import com.gotest.app.core.network.SessionEvent
import com.gotest.app.core.ui.ObserveAsEvents

/**
 * App shell. The bottom bar floats over the content (as in the design) and only shows on
 * the five tab roots; detail screens get the whole window.
 */
@Composable
fun GotestApp(
    appLink: AppLink?,
    onAppLinkConsume: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppShellViewModel = hiltViewModel(),
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    // A saved session opens on Beranda after the app was closed (T-018).
    val startDestination: Any = remember { if (viewModel.startSignedIn) HomeDestination else OnboardingDestination }

    ObserveAsEvents(viewModel.sessionEvents) { event ->
        val target: Any =
            when (event) {
                SessionEvent.SignedOut -> LoginDestination
                SessionEvent.AwaitingConsent -> GuardianConsentDestination
                SessionEvent.UpdateRequired -> UpdateRequiredDestination
            }
        navController.navigate(target) { popUpTo(navController.graph.id) { inclusive = true } }
    }

    // A gotest.id link opens its screen. Resetting the password works signed out; the others wait for sign-in.
    val currentOnAppLinkConsume by rememberUpdatedState(onAppLinkConsume)
    LaunchedEffect(appLink, isSignedIn) {
        val link = appLink ?: return@LaunchedEffect
        if (link !is AppLink.ResetPassword && !isSignedIn) return@LaunchedEffect
        when (link) {
            is AppLink.ResetPassword -> {
                navController.navigate(ResetPasswordDestination(link.token, link.email))
            }

            is AppLink.MainBarengCode -> {
                navController.navigate(MultiplayerDestination(openJoinSheet = true, joinCode = link.code))
            }

            // Gabung ke kelas gets its screen in the next Android task; until then the link only opens the app.
            is AppLink.JoinCode -> {}
        }
        currentOnAppLinkConsume()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }

    Box(modifier = modifier.fillMaxSize()) {
        // Edge to edge: each screen paints its own background behind the status bar (as in the
        // design, where the status bar always takes the page colour) and pads its content.
        GotestNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        )
        if (currentTab != null) {
            GotestBottomBar(
                items =
                    TopLevelDestination.entries.map {
                        GotestBottomBarItem(
                            label = stringResource(it.labelRes),
                            selectedIcon = it.selectedIcon,
                            unselectedIcon = it.unselectedIcon,
                        )
                    },
                selectedIndex = currentTab.ordinal,
                onSelect = { index -> navController.navigateToTab(TopLevelDestination.entries[index]) },
                onAssistantClick = { navController.navigate(AssistantDestination) }.takeIf { AppFeatures.ASSISTANT },
                assistantContentDescription = stringResource(R.string.cd_ai_assistant),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun NavDestination?.isOn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true

/** Switches tabs keeping one copy of each tab and its scroll/state, like the design implies. */
fun NavController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        // Home is the root of the signed-in back stack (auth screens are popped on login).
        popUpTo<HomeDestination> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * "Kembali ke Beranda"/"Lihat Dashboard" after a finished flow: clears everything above Home
 * without saving it, so the flow's screens do not come back when the user reopens a tab.
 */
fun NavController.navigateBackToHome() {
    navigate(HomeDestination) {
        popUpTo<HomeDestination> { inclusive = false }
        launchSingleTop = true
    }
}
