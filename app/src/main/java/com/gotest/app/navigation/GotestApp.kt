package com.gotest.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.ui.join.JoinedMitraSaver
import com.gotest.app.feature.ruang.ui.join.STUDENT
import com.gotest.app.feature.ruang.ui.join.StaffJoinedDialog

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
    // 426 stays for the rest of the run: nothing else opens until the app is updated (T-018 review 4).
    val updateRequired by viewModel.updateRequired.collectAsStateWithLifecycle()
    if (updateRequired) {
        UpdateRequiredRoute(modifier = modifier)
        return
    }
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    // A saved session opens on Beranda after the app was closed (T-018).
    val startDestination: Any = remember { if (viewModel.startSignedIn) HomeDestination else OnboardingDestination }
    val expired by viewModel.expired.collectAsStateWithLifecycle()
    val awaitingConsent by viewModel.awaitingConsent.collectAsStateWithLifecycle()
    val awaitingEmailVerification by viewModel.awaitingEmailVerification.collectAsStateWithLifecycle()
    SessionRedirects(
        navController = navController,
        expired = expired,
        awaitingConsent = awaitingConsent,
        awaitingEmailVerification = awaitingEmailVerification,
        onExpiredHandle = viewModel::consumeExpired,
    )

    // A gotest.id link opens its screen. Resetting the password works signed out; the others wait for sign-in.
    val currentOnAppLinkConsume by rememberUpdatedState(onAppLinkConsume)
    LaunchedEffect(appLink, isSignedIn) {
        val link = appLink ?: return@LaunchedEffect
        if (link !is AppLink.ResetPassword && !isSignedIn) {
            viewModel.keepMitraLink(link)
            return@LaunchedEffect
        }
        if (viewModel.usedBySignUp(link)) {
            // Daftar already joined its Mitra, as the web does not open the link again (A2a-1).
            currentOnAppLinkConsume()
            return@LaunchedEffect
        }
        navController.openAppLink(link)
        currentOnAppLinkConsume()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }
    val joined by viewModel.joined.collectAsStateWithLifecycle()
    SignUpJoinedRedirect(
        navController = navController,
        joined = joined,
        onHome = currentDestination?.hasRoute<HomeDestination>() == true,
        mitraName = { viewModel.joinedMitraName },
        onJoinedConsume = viewModel::consumeJoined,
    )

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

/**
 * The server's session answers, read as state when the app is in the front (T-018 review 4): an expired
 * session opens Masuk, and an account waiting for its parent, or for its email to be proven, sees only that
 * waiting page (and Hapus Akun, which the waiting page opens).
 */
@Composable
private fun SessionRedirects(
    navController: NavHostController,
    expired: Boolean,
    awaitingConsent: Boolean,
    awaitingEmailVerification: Boolean,
    onExpiredHandle: () -> Unit,
) {
    val currentOnExpiredHandle by rememberUpdatedState(onExpiredHandle)
    LaunchedEffect(expired) {
        if (expired) {
            navController.navigate(LoginDestination) { popUpTo(navController.graph.id) { inclusive = true } }
            currentOnExpiredHandle()
        }
    }
    LaunchedEffect(awaitingConsent) {
        val destination = navController.currentDestination
        val onConsentPages =
            destination?.hasRoute<GuardianConsentDestination>() == true ||
                destination?.hasRoute<DeleteAccountDestination>() == true
        if (awaitingConsent && !onConsentPages) {
            navController.navigate(GuardianConsentDestination) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }
    // The email comes first: the server checks it before the parent's approval (DEC-154).
    LaunchedEffect(awaitingEmailVerification) {
        val destination = navController.currentDestination
        val onWaitingPages =
            destination?.hasRoute<EmailVerificationDestination>() == true ||
                destination?.hasRoute<DeleteAccountDestination>() == true
        if (awaitingEmailVerification && !onWaitingPages) {
            navController.navigate(EmailVerificationDestination) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }
}

/**
 * After Daftar from a Mitra link, as the web's `completeMitraJoin` (A2a-1 = a): once Beranda shows (after the
 * email and the parent's approval when those were awaited), a student opens the Ruang of `joined.code`, and
 * staff, whose portal the app does not have, get the A2b-2 dialog.
 */
@Composable
private fun SignUpJoinedRedirect(
    navController: NavHostController,
    joined: JoinedMitra?,
    onHome: Boolean,
    mitraName: () -> String?,
    onJoinedConsume: () -> Unit,
) {
    var staffJoined by rememberSaveable(stateSaver = JoinedMitraSaver) { mutableStateOf<JoinedMitra?>(null) }
    val currentOnJoinedConsume by rememberUpdatedState(onJoinedConsume)
    LaunchedEffect(joined, onHome) {
        val mitra = joined ?: return@LaunchedEffect
        if (!onHome) return@LaunchedEffect
        currentOnJoinedConsume()
        if (mitra.role == STUDENT) {
            navController.navigate(RuangDestination(mitra.code)) { launchSingleTop = true }
        } else {
            staffJoined = mitra
        }
    }
    staffJoined?.let { mitra ->
        StaffJoinedDialog(mitraName = mitraName() ?: mitra.code, onClose = { staffJoined = null })
    }
}

/** The screen of a gotest.id link: resetting the password, a Main Bareng code, a class code, an invitation. */
private fun NavController.openAppLink(link: AppLink) {
    when (link) {
        is AppLink.ResetPassword -> navigate(ResetPasswordDestination(link.token, link.email))
        is AppLink.MainBarengCode -> navigate(MultiplayerDestination(openJoinSheet = true, joinCode = link.code))
        is AppLink.JoinCode -> navigate(JoinCodeDestination(link.code))
        is AppLink.Invitation -> navigate(InvitationDestination(link.token))
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
