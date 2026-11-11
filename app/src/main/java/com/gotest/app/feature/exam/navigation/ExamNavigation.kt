package com.gotest.app.feature.exam.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gotest.app.feature.exam.ui.discussion.DiscussionRoute
import com.gotest.app.feature.exam.ui.exam.ExamRoute
import com.gotest.app.feature.exam.ui.result.ExamResultRoute
import com.gotest.app.feature.exam.ui.summary.DiscussionSummaryRoute
import com.gotest.app.navigation.DiscussionDestination
import com.gotest.app.navigation.DiscussionSummaryDestination
import com.gotest.app.navigation.ExamDestination
import com.gotest.app.navigation.ExamResultDestination
import com.gotest.app.navigation.TopLevelDestination
import com.gotest.app.navigation.navigateBackToHome

/** Pengerjaan soal (latihan and tryout), Hasil, Pembahasan and its summary list. */
fun NavGraphBuilder.examScreens(navController: NavController) {
    composable<ExamDestination> {
        ExamRoute(
            // Answers are saved as they are given, so closing keeps the attempt open for later.
            onClose = navController::navigateUp,
            onShowResult = { attemptId ->
                navController.navigate(ExamResultDestination(attemptId)) {
                    popUpTo<ExamDestination> { inclusive = true }
                }
            },
        )
    }
    composable<ExamResultDestination> { entry ->
        val attemptId = entry.toRoute<ExamResultDestination>().attemptId
        ExamResultRoute(
            onClose = navController::navigateUp,
            onViewDiscussion = {
                navController.navigate(
                    DiscussionDestination(attemptId = attemptId, questionNumber = 1),
                )
            },
            onBackHome = navController::navigateToHomeTab,
        )
    }
    composable<DiscussionDestination> { entry ->
        val attemptId = entry.toRoute<DiscussionDestination>().attemptId
        DiscussionRoute(
            onBack = navController::navigateUp,
            onOpenSummary = {
                navController.navigate(DiscussionSummaryDestination(attemptId)) { launchSingleTop = true }
            },
        )
    }
    composable<DiscussionSummaryDestination> { entry ->
        val attemptId = entry.toRoute<DiscussionSummaryDestination>().attemptId
        DiscussionSummaryRoute(
            onBack = navController::navigateUp,
            onOpenQuestion = { number -> navController.openDiscussion(attemptId, number) },
            onOpenDiscussion = {
                if (navController.previousBackStackEntry?.destination?.hasRoute<DiscussionDestination>() == true) {
                    // Came from Pembahasan: go back to it on the question the student was reading.
                    navController.popBackStack()
                } else {
                    navController.openDiscussion(attemptId, 1)
                }
            },
        )
    }
}

/** Replaces any Pembahasan already on the stack so Back from it returns past the summary. */
private fun NavController.openDiscussion(
    attemptId: String,
    questionNumber: Int,
) {
    navigate(DiscussionDestination(attemptId = attemptId, questionNumber = questionNumber)) {
        popUpTo<DiscussionDestination> { inclusive = true }
    }
}

/** After a finished exam the flow is discarded rather than saved into a tab's back stack. */
private fun NavController.navigateToHomeTab() = navigateBackToHome()
