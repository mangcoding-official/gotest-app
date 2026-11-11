package com.gotest.app.feature.account.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.gotest.app.feature.account.ui.changepassword.ChangePasswordRoute
import com.gotest.app.feature.account.ui.components.AccountNotice
import com.gotest.app.feature.account.ui.deleteaccount.DeleteAccountRoute
import com.gotest.app.feature.account.ui.downloaddata.DownloadDataRoute
import com.gotest.app.feature.account.ui.editprofile.EditProfileRoute
import com.gotest.app.feature.account.ui.help.HelpRoute
import com.gotest.app.feature.account.ui.helptopic.HelpTopicRoute
import com.gotest.app.feature.account.ui.mainlevel.MainLevelRoute
import com.gotest.app.feature.account.ui.personaldata.PersonalDataRoute
import com.gotest.app.feature.account.ui.profile.ProfileNavigation
import com.gotest.app.feature.account.ui.profile.ProfileRoute
import com.gotest.app.feature.account.ui.settings.SettingsNavigation
import com.gotest.app.feature.account.ui.settings.SettingsRoute
import com.gotest.app.feature.account.ui.subscription.SubscriptionRoute
import com.gotest.app.feature.auth.navigation.navigateToOnboardingClearingAll
import com.gotest.app.navigation.ChangePasswordDestination
import com.gotest.app.navigation.DeleteAccountDestination
import com.gotest.app.navigation.DownloadAccountDataDestination
import com.gotest.app.navigation.EditProfileDestination
import com.gotest.app.navigation.HelpDestination
import com.gotest.app.navigation.HelpTopicDestination
import com.gotest.app.navigation.MainLevelDestination
import com.gotest.app.navigation.PersonalDataDestination
import com.gotest.app.navigation.ProfileDestination
import com.gotest.app.navigation.SettingsDestination
import com.gotest.app.navigation.SubscriptionDestination
import com.gotest.app.navigation.UpgradePlanDestination

/** SavedStateHandle key on the previous back-stack entry carrying an [AccountNotice] name. */
private const val ACCOUNT_NOTICE_KEY = "account_notice"

/**
 * Profil (tab root) plus Edit Informasi Akun, Ganti Password, Hapus Akun, Pengaturan (with
 * Jenjang Utama, Data Pribadi and Unduh Data Akun), Langganan, Bantuan & Support and
 * Bantuan / Detail Topik.
 */
fun NavGraphBuilder.accountScreens(navController: NavController) {
    composable<ProfileDestination> { entry ->
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
            notice = entry.accountNotice(),
            onNoticeConsume = entry::consumeAccountNotice,
            onSignOutSuccess = navController::navigateToOnboardingClearingAll,
        )
    }
    composable<EditProfileDestination> {
        EditProfileRoute(
            onBack = navController::navigateUp,
            onSaveSuccess = { navController.navigateUpWithNotice(AccountNotice.ProfileSaved) },
        )
    }
    composable<ChangePasswordDestination> {
        ChangePasswordRoute(
            onBack = navController::navigateUp,
            onChangeSuccess = { navController.navigateUpWithNotice(AccountNotice.PasswordChanged) },
        )
    }
    composable<DeleteAccountDestination> {
        DeleteAccountRoute(
            onBack = navController::navigateUp,
            onDeleteSuccess = navController::navigateToOnboardingClearingAll,
        )
    }
    settingsScreens(navController)
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

/** Pengaturan and the Jenjang Utama, Data Pribadi and Unduh Data Akun screens it opens. */
private fun NavGraphBuilder.settingsScreens(navController: NavController) {
    composable<SettingsDestination> { entry ->
        SettingsRoute(
            onBack = navController::navigateUp,
            navigation =
                SettingsNavigation(
                    onOpenMainLevel = { navController.navigate(MainLevelDestination) },
                    onOpenSubscription = { navController.navigate(SubscriptionDestination) },
                    onOpenPersonalData = { navController.navigate(PersonalDataDestination) },
                    onOpenDownloadData = { navController.navigate(DownloadAccountDataDestination) },
                ),
            notice = entry.accountNotice(),
            onNoticeConsume = entry::consumeAccountNotice,
        )
    }
    composable<MainLevelDestination> {
        MainLevelRoute(
            onBack = navController::navigateUp,
            onSaveSuccess = { navController.navigateUpWithNotice(AccountNotice.MainLevelSaved) },
        )
    }
    composable<PersonalDataDestination> { entry ->
        PersonalDataRoute(
            onBack = navController::navigateUp,
            onEditProfile = { navController.navigate(EditProfileDestination) },
            notice = entry.accountNotice(),
            onNoticeConsume = entry::consumeAccountNotice,
        )
    }
    composable<DownloadAccountDataDestination> {
        DownloadDataRoute(
            onBack = navController::navigateUp,
            onRequestSuccess = { navController.navigateUpWithNotice(AccountNotice.DataCopyRequested) },
        )
    }
}

/** Closes the current screen and lets the previous entry show [notice] as a snackbar. */
private fun NavController.navigateUpWithNotice(notice: AccountNotice) {
    previousBackStackEntry?.savedStateHandle?.set(ACCOUNT_NOTICE_KEY, notice.name)
    navigateUp()
}

/** The notice a closed screen left for this entry, if any. */
@Composable
private fun NavBackStackEntry.accountNotice(): AccountNotice? {
    val name by savedStateHandle.getStateFlow<String?>(ACCOUNT_NOTICE_KEY, null).collectAsStateWithLifecycle()
    return AccountNotice.entries.firstOrNull { it.name == name }
}

private fun NavBackStackEntry.consumeAccountNotice() {
    savedStateHandle[ACCOUNT_NOTICE_KEY] = null
}
