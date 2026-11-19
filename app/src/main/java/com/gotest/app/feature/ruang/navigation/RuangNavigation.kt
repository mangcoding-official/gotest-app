package com.gotest.app.feature.ruang.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.auth.navigation.navigateToOnboardingClearingAll
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.ui.detail.ExamDetailRoute
import com.gotest.app.feature.ruang.ui.exams.ExamCardAction
import com.gotest.app.feature.ruang.ui.exams.ExamListRoute
import com.gotest.app.feature.ruang.ui.join.InvitationRoute
import com.gotest.app.feature.ruang.ui.join.JoinCodeRoute
import com.gotest.app.feature.ruang.ui.join.JoinWithCodeRoute
import com.gotest.app.feature.ruang.ui.result.ExamResultRoute
import com.gotest.app.feature.ruang.ui.space.RuangRoute
import com.gotest.app.feature.ruang.ui.work.ExamWorkRoute
import com.gotest.app.navigation.HomeDestination
import com.gotest.app.navigation.InvitationDestination
import com.gotest.app.navigation.JoinCodeDestination
import com.gotest.app.navigation.JoinWithCodeDestination
import com.gotest.app.navigation.LoginDestination
import com.gotest.app.navigation.RegisterDestination
import com.gotest.app.navigation.RuangDestination
import com.gotest.app.navigation.UjianDetailDestination
import com.gotest.app.navigation.UjianListDestination
import com.gotest.app.navigation.UjianResultDestination
import com.gotest.app.navigation.UjianWorkDestination

/** Ruang Mitra, Ujian, Gabung ke kelas, and invitations for students (T-026 A2b). */
fun NavGraphBuilder.ruangScreens(navController: NavController) {
    composable<RuangDestination> {
        RuangRoute(
            onBack = navController::navigateUp,
            onOpenExams = { code -> navController.navigate(UjianListDestination(code)) },
        )
    }
    composable<UjianListDestination> {
        ExamListRoute(
            onBack = navController::navigateUp,
            onAction = { code, action ->
                when (action) {
                    is ExamCardAction.Result -> navController.navigate(UjianResultDestination(code, action.attemptId))

                    // The work screen asks to pin the screen first (API-K8 A), so "Lanjutkan" goes straight there.
                    is ExamCardAction.Continue -> navController.navigate(UjianWorkDestination(code, action.attemptId))

                    is ExamCardAction.Detail -> navController.navigate(UjianDetailDestination(code, action.scheduleId))
                }
            },
        )
    }
    composable<UjianDetailDestination> {
        ExamDetailRoute(
            onBack = navController::navigateUp,
            onOpenWork = { code, attemptId ->
                navController.navigate(UjianWorkDestination(code, attemptId)) {
                    popUpTo<UjianDetailDestination> { inclusive = true }
                }
            },
            onOpenResult = { code, attemptId -> navController.navigate(UjianResultDestination(code, attemptId)) },
        )
    }
    composable<UjianWorkDestination> {
        ExamWorkRoute(
            onOpenResult = { code, attemptId ->
                navController.navigate(UjianResultDestination(code, attemptId)) {
                    popUpTo<UjianWorkDestination> { inclusive = true }
                }
            },
            onLeave = navController::navigateUp,
        )
    }
    composable<UjianResultDestination> {
        ExamResultRoute(
            onBackToList = { code ->
                if (!navController.popBackStack<UjianListDestination>(inclusive = false)) {
                    navController.navigate(UjianListDestination(code)) {
                        popUpTo<UjianResultDestination> { inclusive = true }
                    }
                }
            },
        )
    }
    joinScreens(navController)
}

private fun NavGraphBuilder.joinScreens(navController: NavController) {
    composable<JoinWithCodeDestination> {
        JoinWithCodeRoute(
            onBack = navController::navigateUp,
            onOpenCode = { code -> navController.navigate(JoinCodeDestination(code)) },
        )
    }
    composable<JoinCodeDestination> {
        JoinCodeRoute(
            onBack = navController::navigateUp,
            onJoinSuccess = navController::openJoined,
            onSignIn = { navController.navigate(LoginDestination) },
            onRegister = { navController.navigate(RegisterDestination) },
        )
    }
    composable<InvitationDestination> {
        InvitationRoute(
            onBack = navController::navigateUp,
            onJoinSuccess = navController::openJoined,
            onSignOutSuccess = navController::navigateToOnboardingClearingAll,
            onSignIn = { navController.navigate(LoginDestination) },
            onRegister = { navController.navigate(RegisterDestination) },
        )
    }
}

/**
 * After joining: a student opens the Mitra space, as the web does. A staff role (an event code of the
 * account's own Mitra) would open the portal, which the app does not have yet (A4): the joined dialog
 * points to gotest.id, then back to Beranda (A2b-2 = a).
 */
private fun NavController.openJoined(joined: JoinedMitra) {
    // The join screens leave the back stack: Back from the space returns to where joining started.
    if (!popBackStack<JoinWithCodeDestination>(inclusive = true) &&
        !popBackStack<JoinCodeDestination>(inclusive = true)
    ) {
        popBackStack<InvitationDestination>(inclusive = true)
    }
    if (joined.role == STUDENT) {
        navigate(RuangDestination(joined.code)) { launchSingleTop = true }
    } else {
        navigate(HomeDestination) { launchSingleTop = true }
    }
}

private const val STUDENT = "siswa"
