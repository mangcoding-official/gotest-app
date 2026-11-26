package com.gotest.app.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.auth.ui.childlogin.ChildLoginRoute
import com.gotest.app.feature.auth.ui.consent.GuardianConsentRoute
import com.gotest.app.feature.auth.ui.emailverification.EmailVerificationRoute
import com.gotest.app.feature.auth.ui.forgotpassword.ForgotPasswordRoute
import com.gotest.app.feature.auth.ui.login.LoginRoute
import com.gotest.app.feature.auth.ui.onboarding.OnboardingRoute
import com.gotest.app.feature.auth.ui.phoneregister.PhoneRegisterRoute
import com.gotest.app.feature.auth.ui.register.RegisterRoute
import com.gotest.app.feature.auth.ui.resetpassword.ResetPasswordRoute
import com.gotest.app.navigation.ChildLoginDestination
import com.gotest.app.navigation.DeleteAccountDestination
import com.gotest.app.navigation.EmailVerificationDestination
import com.gotest.app.navigation.ForgotPasswordDestination
import com.gotest.app.navigation.GuardianConsentDestination
import com.gotest.app.navigation.HomeDestination
import com.gotest.app.navigation.LoginDestination
import com.gotest.app.navigation.OnboardingDestination
import com.gotest.app.navigation.PhoneRegisterDestination
import com.gotest.app.navigation.RegisterDestination
import com.gotest.app.navigation.ResetPasswordDestination

/** Onboarding, Masuk, Daftar and Lupa Password. */
fun NavGraphBuilder.authScreens(navController: NavController) {
    composable<OnboardingDestination> {
        OnboardingRoute(
            onSignIn = { navController.navigate(LoginDestination) },
            onRegister = { navController.navigate(RegisterDestination) },
        )
    }
    composable<LoginDestination> {
        LoginRoute(
            onSignInSuccess = navController::navigateToHomeClearingAuth,
            onForgotPassword = { navController.navigate(ForgotPasswordDestination) },
            onRegister = {
                navController.navigate(RegisterDestination) { popUpTo<LoginDestination> { inclusive = true } }
            },
            onChildSignIn = { navController.navigate(ChildLoginDestination) },
            onPhoneRegister = { phone -> navController.navigate(PhoneRegisterDestination(phone)) },
        )
    }
    composable<ChildLoginDestination> {
        ChildLoginRoute(
            onSignInSuccess = navController::navigateToHomeClearingAuth,
            onOtherSignIn = {
                navController.navigate(LoginDestination) { popUpTo<ChildLoginDestination> { inclusive = true } }
            },
            onBack = navController::navigateUp,
        )
    }
    composable<ResetPasswordDestination> {
        ResetPasswordRoute(
            onBackToLogin = {
                navController.navigate(LoginDestination) { popUpTo<ResetPasswordDestination> { inclusive = true } }
            },
        )
    }
    composable<RegisterDestination> {
        RegisterRoute(
            onRegisterSuccess = navController::navigateToHomeClearingAuth,
            onSignIn = {
                navController.navigate(LoginDestination) { popUpTo<RegisterDestination> { inclusive = true } }
            },
            onBack = navController::navigateUp,
            onPhoneRegister = { phone -> navController.navigate(PhoneRegisterDestination(phone)) },
            onAwaitConsent = navController::navigateToConsentClearingAuth,
        )
    }
    composable<PhoneRegisterDestination> {
        PhoneRegisterRoute(
            onRegisterSuccess = navController::navigateToHomeClearingAuth,
            onChangeNumber = navController::navigateUp,
            onAwaitConsent = navController::navigateToConsentClearingAuth,
        )
    }
    composable<GuardianConsentDestination> {
        GuardianConsentRoute(
            onSignOutSuccess = navController::navigateToOnboardingClearingAll,
            onApprove = navController::navigateToHomeClearingAuth,
            onDeleteAccount = { navController.navigate(DeleteAccountDestination(awaitingConsent = true)) },
        )
    }
    composable<EmailVerificationDestination> {
        EmailVerificationRoute(
            // The Mitra the sign-up link joined opens from Beranda, as the web goes to it (DEC-154, A2a-1).
            onAccountOpen = navController::navigateToHomeClearingAuth,
            onSignOutSuccess = navController::navigateToOnboardingClearingAll,
            onDeleteAccount = { navController.navigate(DeleteAccountDestination(awaitingConsent = true)) },
        )
    }
    composable<ForgotPasswordDestination> {
        ForgotPasswordRoute(onBackToLogin = navController::navigateUp)
    }
}

/**
 * After sign-in Home becomes the root, so Back leaves the app instead of returning to auth. The whole
 * stack goes, also when Masuk was opened by a 401 without Onboarding below it (T-018).
 */
fun NavController.navigateToHomeClearingAuth() {
    navigate(HomeDestination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** An under-age account waits for the parent there, with nothing behind it (Aturan 2.15). */
fun NavController.navigateToConsentClearingAuth() {
    navigate(GuardianConsentDestination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/** Sign-out and account deletion return to Onboarding with an empty back stack. */
fun NavController.navigateToOnboardingClearingAll() {
    navigate(OnboardingDestination) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
