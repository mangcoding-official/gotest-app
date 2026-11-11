package com.gotest.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomBar
import com.gotest.app.core.designsystem.component.GotestBottomBarItem

/**
 * App shell. The bottom bar floats over the content (as in the design) and only shows on
 * the five tab roots; detail screens get the whole window.
 */
@Composable
fun GotestApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }

    Box(modifier = modifier.fillMaxSize()) {
        // Edge to edge: each screen paints its own background behind the status bar (as in the
        // design, where the status bar always takes the page colour) and pads its content.
        GotestNavHost(
            navController = navController,
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
                onAssistantClick = { navController.navigate(AssistantDestination) },
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
