package com.gotest.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import kotlin.reflect.KClass

/** Tabs of the bottom navigation, in the order of the design's "Mobile/Navbar". */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val selectedIcon: Int,
    @param:DrawableRes val unselectedIcon: Int,
) {
    Home(HomeDestination, HomeDestination::class, R.string.nav_home, GotestIcons.Bold.Home2, GotestIcons.Linear.Home2),
    StudyPackage(
        StudyPackageDestination,
        StudyPackageDestination::class,
        R.string.nav_study_package,
        GotestIcons.Bold.TaskSquare,
        GotestIcons.Linear.TaskSquare,
    ),
    History(
        HistoryDestination,
        HistoryDestination::class,
        R.string.nav_history,
        GotestIcons.Bold.Directbox,
        GotestIcons.Linear.Directbox,
    ),
    Leaderboard(
        LeaderboardDestination,
        LeaderboardDestination::class,
        R.string.nav_leaderboard,
        GotestIcons.Bold.Leaderboard,
        GotestIcons.Linear.Leaderboard,
    ),
    Profile(
        ProfileDestination,
        ProfileDestination::class,
        R.string.nav_profile,
        GotestIcons.Bold.User,
        GotestIcons.Linear.User,
    ),
}
