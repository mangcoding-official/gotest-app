package com.gotest.app.feature.practice.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gotest.app.feature.practice.ui.catalog.PracticeCategoryRoute
import com.gotest.app.feature.practice.ui.catalog.PracticeNavigation
import com.gotest.app.feature.practice.ui.catalog.PracticeSubjectRoute
import com.gotest.app.feature.practice.ui.result.PracticeResultRoute
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerRoute
import com.gotest.app.navigation.HistoryDetailDestination
import com.gotest.app.navigation.PracticeCategoryDestination
import com.gotest.app.navigation.PracticeResultDestination
import com.gotest.app.navigation.PracticeRunnerDestination
import com.gotest.app.navigation.PracticeSubjectDestination

/** Where the Latihan catalog leads, at every level and from the Paket tab. */
fun NavController.practiceNavigation() =
    PracticeNavigation(
        onOpenCategory = { categoryId -> navigate(PracticeCategoryDestination(categoryId)) { launchSingleTop = true } },
        onOpenSubject = { categoryId, subjectId -> navigate(PracticeSubjectDestination(categoryId, subjectId)) },
        onOpenRunner = { attemptId, name -> navigate(PracticeRunnerDestination(attemptId, name)) },
        onOpenResult = { attemptId, name -> navigate(PracticeResultDestination(attemptId, name)) },
        onOpenHistory = { attemptId -> navigate(HistoryDetailDestination(attemptId.toString())) },
    )

/** Latihan (T-026 A2c): a category, its subtest, the runner, and the pembahasan. */
fun NavGraphBuilder.practiceScreens(navController: NavController) {
    composable<PracticeCategoryDestination> {
        PracticeCategoryRoute(onBack = navController::navigateUp, navigation = navController.practiceNavigation())
    }
    composable<PracticeSubjectDestination> {
        PracticeSubjectRoute(onBack = navController::navigateUp, navigation = navController.practiceNavigation())
    }
    composable<PracticeRunnerDestination> { entry ->
        val route = entry.toRoute<PracticeRunnerDestination>()
        PracticeRunnerRoute(
            packageName = route.packageName,
            onOpenResult = {
                navController.navigate(PracticeResultDestination(route.attemptId, route.packageName)) {
                    popUpTo<PracticeRunnerDestination> { inclusive = true }
                }
            },
            onLeave = navController::navigateUp,
        )
    }
    composable<PracticeResultDestination> { entry ->
        PracticeResultRoute(
            packageName = entry.toRoute<PracticeResultDestination>().packageName,
            onBack = navController::navigateUp,
        )
    }
}
