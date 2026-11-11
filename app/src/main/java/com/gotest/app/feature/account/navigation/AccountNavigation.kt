package com.gotest.app.feature.account.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.account.ui.changepassword.ChangePasswordRoute
import com.gotest.app.feature.account.ui.deleteaccount.DeleteAccountRoute
import com.gotest.app.feature.account.ui.editprofile.EditProfileRoute
import com.gotest.app.feature.account.ui.help.HelpRoute
import com.gotest.app.feature.account.ui.helptopic.HelpTopicRoute
import com.gotest.app.feature.account.ui.profile.ProfileNavigation
import com.gotest.app.feature.account.ui.profile.ProfileNotice
import com.gotest.app.feature.account.ui.profile.ProfileRoute
import com.gotest.app.feature.account.ui.settings.SettingsRoute
import com.gotest.app.feature.account.ui.subscription.SubscriptionRoute
import com.gotest.app.feature.auth.navigation.navigateToOnboardingClearingAll
import com.gotest.app.navigation.ChangePasswordDestination
import com.gotest.app.navigation.DeleteAccountDestination
import com.gotest.app.navigation.EditProfileDestination
import com.gotest.app.navigation.HelpDestination
import com.gotest.app.navigation.HelpTopicDestination
import com.gotest.app.navigation.ProfileDestination
import com.gotest.app.navigation.SettingsDestination
import com.gotest.app.navigation.SubscriptionDestination
import com.gotest.app.navigation.UpgradePlanDestination

/** SavedStateHandle key on Profil's back-stack entry carrying a [ProfileNotice] name. */
private const val PROFILE_NOTICE_KEY = "account_profile_notice"

/**
 * Profil (tab root) plus Edit Informasi Akun, Ganti Password, Hapus Akun, Pengaturan,
 * Langganan, Bantuan & Support and Bantuan / Detail Topik.
 */
fun NavGraphBuilder.accountScreens(navController: NavController) {
    composable<ProfileDestination> { entry ->
        val noticeName by entry.savedStateHandle
            .getStateFlow<String?>(PROFILE_NOTICE_KEY, null)
            .collectAsStateWithLifecycle()
        ProfileRoute(
            navigation =
                ProfileNavigation(
                    onOpenSettings = { navController.navigate(SettingsDestination) },
                    onUpgrade = { navController.navigate(UpgradePlanDestination) },
                    onOpenSubscription = { navController.navigate(SubscriptionDestination) },
                    onEditProfile = { navController.navigate(EditProfileDestination) },
                    onChangePassword = { navController.navigate(ChangePasswordDestination) },
                    onOpenHelp = { navController.navigate(HelpDestination) },
                    onDeleteAccount = { navController.navigate(DeleteAccountDestination) },
                ),
            notice = ProfileNotice.entries.firstOrNull { it.name == noticeName },
            onNoticeConsume = { entry.savedStateHandle[PROFILE_NOTICE_KEY] = null },
            onSignOutSuccess = navController::navigateToOnboardingClearingAll,
        )
    }
    composable<EditProfileDestination> {
        EditProfileRoute(
            onBack = navController::navigateUp,
            onSaveSuccess = { navController.navigateUpWithNotice(ProfileNotice.ProfileSaved) },
        )
    }
    composable<ChangePasswordDestination> {
        ChangePasswordRoute(
            onBack = navController::navigateUp,
            onChangeSuccess = { navController.navigateUpWithNotice(ProfileNotice.PasswordChanged) },
        )
    }
    composable<DeleteAccountDestination> {
        DeleteAccountRoute(
            onBack = navController::navigateUp,
            onDeleteSuccess = navController::navigateToOnboardingClearingAll,
        )
    }
    composable<SettingsDestination> {
        SettingsRoute(
            onBack = navController::navigateUp,
            onOpenSubscription = { navController.navigate(SubscriptionDestination) },
        )
    }
    composable<SubscriptionDestination> {
        SubscriptionRoute(
            onBack = navController::navigateUp,
            onUpgrade = { navController.navigate(UpgradePlanDestination) },
        )
    }
    composable<HelpDestination> {
        HelpRoute(
            onBack = navController::navigateUp,
            onOpenTopic = { topicId -> navController.navigate(HelpTopicDestination(topicId)) },
        )
    }
    composable<HelpTopicDestination> {
        HelpTopicRoute(onBack = navController::navigateUp)
    }
}

/** Closes the current screen and lets Profil (the previous entry) show [notice] as a snackbar. */
private fun NavController.navigateUpWithNotice(notice: ProfileNotice) {
    previousBackStackEntry?.savedStateHandle?.set(PROFILE_NOTICE_KEY, notice.name)
    navigateUp()
}
