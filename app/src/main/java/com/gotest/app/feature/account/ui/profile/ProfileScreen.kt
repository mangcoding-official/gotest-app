package com.gotest.app.feature.account.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.config.AppFeatures
import com.gotest.app.core.designsystem.component.GotestDialog
import com.gotest.app.core.designsystem.component.GotestDialogContent
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestIconBadgeColors
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountMenuRow
import com.gotest.app.feature.account.ui.components.AccountMenuTone
import com.gotest.app.feature.account.ui.components.AccountNotice
import com.gotest.app.feature.account.ui.components.AccountNoticeEffect
import com.gotest.app.feature.account.ui.components.AccountPreviewData
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.components.ManagedAccountNote
import com.gotest.app.feature.account.ui.components.PhotoMessageHost
import com.gotest.app.feature.account.ui.components.PhotoSheetUiState
import com.gotest.app.feature.account.ui.components.ProfilePhotoCaptureDialog
import com.gotest.app.feature.account.ui.components.ProfilePhotoSheet
import com.gotest.app.feature.account.ui.components.ProfilePhotoSheetContent
import com.gotest.app.feature.account.ui.components.rememberGalleryPhotoPicker
import com.gotest.app.feature.account.ui.components.rememberTakePhotoAction
import com.gotest.app.feature.ruang.domain.MitraMembership
import kotlinx.coroutines.launch

/** Navigation out of Profil, grouped so the screen signature stays readable. */
data class ProfileNavigation(
    /** Null while Pengaturan is hidden (AppFeatures.ACCOUNT_SETTINGS). */
    val onOpenSettings: (() -> Unit)?,
    /** Null while payments are hidden (AppFeatures): the plan banner only shows the plan. */
    val onUpgrade: (() -> Unit)?,
    val onOpenSubscription: (() -> Unit)?,
    val onEditProfile: () -> Unit,
    val onChangePassword: () -> Unit,
    val onOpenHelp: () -> Unit,
    val onDeleteAccount: () -> Unit,
    val onOpenPhone: () -> Unit,
    val onOpenSessions: () -> Unit,
    /** A Mitra space by its code (T-026 A2b). */
    val onOpenRuang: (String) -> Unit = {},
    /** "Gabung ke kelas"; null for a child account (DEC-145). */
    val onJoinWithCode: (() -> Unit)? = null,
)

@Composable
fun ProfileRoute(
    navigation: ProfileNavigation,
    notice: AccountNotice?,
    onNoticeConsume: () -> Unit,
    onSignOutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    noticeText: String? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetSnackbarHostState = remember { SnackbarHostState() }
    val captureSnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    AccountNoticeEffect(
        notice = notice,
        onConsume = onNoticeConsume,
        snackbarHostState = snackbarHostState,
        text = noticeText,
    )
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            ProfileEffect.SignedOut -> {
                onSignOutSuccess()
            }

            is ProfileEffect.ShowMessage -> {
                val host =
                    when (effect.host) {
                        PhotoMessageHost.Screen -> snackbarHostState
                        PhotoMessageHost.Sheet -> sheetSnackbarHostState
                        PhotoMessageHost.Capture -> captureSnackbarHostState
                    }
                scope.launch { host.showSnackbar(resources.getString(effect.messageRes)) }
            }

            is ProfileEffect.ShowText -> {
                val host =
                    when (effect.host) {
                        PhotoMessageHost.Screen -> snackbarHostState
                        PhotoMessageHost.Sheet -> sheetSnackbarHostState
                        PhotoMessageHost.Capture -> captureSnackbarHostState
                    }
                scope.launch { host.showSnackbar(effect.message ?: resources.getString(R.string.state_error_message)) }
            }
        }
    }
    val pickPhoto = rememberGalleryPhotoPicker { viewModel.onEvent(ProfileUiEvent.PhotoPick(it)) }
    val takePhoto = rememberTakePhotoAction(sheetSnackbarHostState) { viewModel.onEvent(ProfileUiEvent.TakePhoto) }
    ProfileScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        navigation = navigation,
        onTakePhoto = takePhoto,
        onPickPhoto = pickPhoto,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        sheetSnackbarHostState = sheetSnackbarHostState,
        captureSnackbarHostState = captureSnackbarHostState,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    navigation: ProfileNavigation,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    sheetSnackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    captureSnackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // Tab roots draw under the floating bottom bar.
    val bottomInset =
        GotestTheme.sizes.bottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(GotestTheme.colors.bgSubtle),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProfileHeader(onOpenSettings = navigation.onOpenSettings)
            when (uiState) {
                ProfileUiState.Loading -> {
                    GotestLoadingView(modifier = Modifier.padding(bottom = bottomInset))
                }

                ProfileUiState.Error -> {
                    GotestErrorView(
                        onRetry = { onEvent(ProfileUiEvent.Retry) },
                        modifier = Modifier.fillMaxSize().padding(bottom = bottomInset),
                    )
                }

                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = uiState,
                        onEvent = onEvent,
                        navigation = navigation,
                        bottomInset = bottomInset,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset),
        )
        if (uiState is ProfileUiState.Success) {
            ProfileOverlays(
                state = uiState,
                onEvent = onEvent,
                onTakePhoto = onTakePhoto,
                onPickPhoto = onPickPhoto,
                sheetSnackbarHostState = sheetSnackbarHostState,
                captureSnackbarHostState = captureSnackbarHostState,
            )
        }
    }
}

/** "header": large left-aligned title plus the settings button while Pengaturan is shown. */
@Composable
private fun ProfileHeader(
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = GotestTheme.sizes.appBarHeight)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.account_profile_title),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            color = GotestTheme.colors.textPrimary,
        )
        onOpenSettings?.let { onClick ->
            GotestIconButton(
                icon = GotestIcons.Linear.Setting,
                contentDescription = stringResource(R.string.account_profile_settings_cd),
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    onEvent: (ProfileUiEvent) -> Unit,
    navigation: ProfileNavigation,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = bottomInset + spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        val profile = state.profile
        ProfileCard(
            profile = profile,
            onAvatarClick = { onEvent(ProfileUiEvent.AvatarClick) }.takeUnless { profile.isChild },
        )
        profile.managedBy?.let { ManagedAccountNote(mitra = it) }
        PlanBanner(
            subscription = state.profile.subscription,
            daysRemaining = state.daysRemaining,
            onUpgrade = navigation.onUpgrade,
            onOpenSubscription = navigation.onOpenSubscription,
        )
        MitraSection(
            spaces = state.spaces,
            onOpenRuang = navigation.onOpenRuang,
            onJoinWithCode = navigation.onJoinWithCode.takeUnless { profile.isChild },
        )
        ProfileMenu(
            profile = profile,
            phoneSignIn = state.phoneSignIn,
            navigation = navigation,
            onSignOut = { onEvent(ProfileUiEvent.SignOutClick) },
        )
    }
}

/**
 * The web's account menu rows for Mitra: "Ruang {nama}" per space where the account is a student,
 * then "Gabung ke kelas" (not for a child account). "Portal {nama}" comes with the portal (A4).
 */
@Composable
private fun MitraSection(
    spaces: List<MitraMembership>,
    onOpenRuang: (String) -> Unit,
    onJoinWithCode: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (spaces.isEmpty() && onJoinWithCode == null) return
    AccountListCard(modifier = modifier) {
        spaces.forEach { space ->
            AccountMenuRow(
                label = stringResource(R.string.ruang_menu_space, space.name),
                icon = GotestIcons.Linear.Buildings,
                onClick = { onOpenRuang(space.code) },
            )
        }
        onJoinWithCode?.let {
            AccountMenuRow(
                label = stringResource(R.string.ruang_menu_join),
                icon = GotestIcons.Linear.AddCircle,
                onClick = it,
            )
        }
    }
}

/**
 * Akun, Lainnya and the red Keluar / Hapus Akun group. A child account has no edit, number, password,
 * Sesi Aktif, or delete rows; its parent or teacher keeps them (Aturan 11.30).
 */
@Composable
private fun ProfileMenu(
    profile: UserProfile,
    phoneSignIn: Boolean,
    navigation: ProfileNavigation,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val adult = !profile.isChild
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        AccountSection(profile = profile, phoneSignIn = phoneSignIn, navigation = navigation)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AccountSectionTitle(title = stringResource(R.string.account_profile_section_other), muted = true)
            AccountListCard {
                AccountMenuRow(
                    label = stringResource(R.string.account_profile_help),
                    icon = GotestIcons.Linear.MessageQuestion,
                    onClick = navigation.onOpenHelp,
                )
                navigation.onOpenSettings?.let { onClick ->
                    AccountMenuRow(
                        label = stringResource(R.string.account_profile_settings),
                        icon = GotestIcons.Linear.Setting,
                        onClick = onClick,
                    )
                }
            }
        }
        AccountListCard {
            AccountMenuRow(
                label = stringResource(R.string.account_profile_sign_out),
                icon = GotestIcons.Linear.Logout,
                onClick = onSignOut,
                tone = AccountMenuTone.Danger,
            )
            if (adult) {
                AccountMenuRow(
                    label = stringResource(R.string.account_profile_delete),
                    icon = GotestIcons.Linear.UserMinus,
                    onClick = navigation.onDeleteAccount,
                    tone = AccountMenuTone.Danger,
                )
            }
        }
    }
}

/** The Akun rows; [ProfileMenu] says which a child account leaves out. */
@Composable
private fun AccountSection(
    profile: UserProfile,
    phoneSignIn: Boolean,
    navigation: ProfileNavigation,
    modifier: Modifier = Modifier,
) {
    val adult = !profile.isChild
    // A child account has none of these rows while Notifikasi is hidden, so the section goes too.
    if (!adult && !AppFeatures.NOTIFICATION_SETTINGS) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm)) {
        AccountSectionTitle(title = stringResource(R.string.account_profile_section_account), muted = true)
        AccountListCard {
            if (adult) {
                AccountMenuRow(
                    label = stringResource(R.string.account_profile_edit),
                    icon = GotestIcons.Linear.UserEdit,
                    onClick = navigation.onEditProfile,
                )
            }
            // As on the web: the number row shows while codes are on, or while the account has a number.
            if (adult && (phoneSignIn || profile.phone != null)) {
                AccountMenuRow(
                    label =
                        stringResource(
                            if (profile.phone !=
                                null
                            ) {
                                R.string.account_profile_phone
                            } else {
                                R.string.account_profile_phone_add
                            },
                        ),
                    icon = GotestIcons.Linear.Message,
                    onClick = navigation.onOpenPhone,
                )
            }
            // An account without an email signs in with a WhatsApp code; it has no password to change.
            if (adult && profile.hasEmail) {
                AccountMenuRow(
                    label = stringResource(R.string.account_profile_change_password),
                    icon = GotestIcons.Linear.Lock,
                    onClick = navigation.onChangePassword,
                )
            }
            if (adult) {
                AccountMenuRow(
                    label = stringResource(R.string.account_profile_sessions),
                    icon = GotestIcons.Linear.MenuBoard,
                    onClick = navigation.onOpenSessions,
                )
            }
            if (AppFeatures.NOTIFICATION_SETTINGS) {
                AccountMenuRow(
                    label = stringResource(R.string.account_profile_notifications),
                    icon = GotestIcons.Linear.Notification,
                    onClick = navigation.onOpenSettings ?: {},
                )
            }
        }
    }
}

/** The "Foto Profil" sheet with its camera flow, and the sign-out confirmation. */
@Composable
private fun ProfileOverlays(
    state: ProfileUiState.Success,
    onEvent: (ProfileUiEvent) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    sheetSnackbarHostState: SnackbarHostState,
    captureSnackbarHostState: SnackbarHostState,
) {
    state.photoSheet?.let { sheet ->
        ProfilePhotoSheet(
            currentPhoto = state.profile.avatarUrl,
            state = sheet,
            snackbarHostState = sheetSnackbarHostState,
            onTakePhoto = onTakePhoto,
            onPickFromGallery = onPickPhoto,
            onSave = { onEvent(ProfileUiEvent.SavePhoto) },
            onDismiss = { onEvent(ProfileUiEvent.DismissPhotoSheet) },
            onDelete = { onEvent(ProfileUiEvent.DeletePhoto) },
        )
        sheet.capture?.let { capture ->
            ProfilePhotoCaptureDialog(
                state = capture,
                onEvent = { onEvent(ProfileUiEvent.PhotoCapture(it)) },
                onPickFromGallery = onPickPhoto,
                snackbarHostState = captureSnackbarHostState,
            )
        }
    }
    if (state.showSignOutDialog) {
        GotestDialog(
            title = stringResource(R.string.account_sign_out_title),
            message = stringResource(R.string.account_sign_out_message),
            confirmText = stringResource(R.string.account_sign_out_confirm),
            onConfirm = { onEvent(ProfileUiEvent.ConfirmSignOut) },
            onDismissRequest = { onEvent(ProfileUiEvent.DismissSignOut) },
            icon = GotestIcons.Linear.Logout,
            iconColors = signOutIconColors(),
            dismissText = stringResource(R.string.account_cancel),
        )
    }
}

@Composable
private fun signOutIconColors() =
    GotestIconBadgeColors(container = GotestTheme.colors.statusDangerSoft, content = GotestTheme.colors.statusDanger)

private val previewNavigation =
    ProfileNavigation(
        onOpenSettings = {},
        onUpgrade = {},
        onOpenSubscription = {},
        onEditProfile = {},
        onChangePassword = {},
        onOpenHelp = {},
        onDeleteAccount = {},
        onOpenPhone = {},
        onOpenSessions = {},
    )

@Composable
private fun ProfilePreviewScreen(uiState: ProfileUiState) {
    ProfileScreen(
        uiState = uiState,
        onEvent = {},
        navigation = previewNavigation,
        onTakePhoto = {},
        onPickPhoto = {},
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenFreePreview() {
    GotestTheme {
        ProfilePreviewScreen(ProfileUiState.Success(profile = AccountPreviewData.profile, daysRemaining = 30))
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 1100)
@Composable
private fun ProfileScreenPhonePreview() {
    GotestTheme {
        ProfilePreviewScreen(
            ProfileUiState.Success(
                profile = AccountPreviewData.profile.copy(phone = "0812-3456-7890"),
                phoneSignIn = true,
                daysRemaining = 30,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenChildPreview() {
    GotestTheme {
        ProfilePreviewScreen(
            ProfileUiState.Success(
                profile =
                    AccountPreviewData.profile.copy(
                        fullName = "Raka",
                        email = "",
                        avatarUrl = null,
                        managedBy = "Keluarga Pratama",
                    ),
                phoneSignIn = true,
                daysRemaining = 30,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenPremiumPreview() {
    GotestTheme {
        ProfilePreviewScreen(
            ProfileUiState.Success(
                profile = AccountPreviewData.profile.copy(subscription = AccountPreviewData.premiumSubscription),
                daysRemaining = 365,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenLoadingPreview() {
    GotestTheme { ProfilePreviewScreen(ProfileUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenErrorPreview() {
    GotestTheme { ProfilePreviewScreen(ProfileUiState.Error) }
}

/** Frame 10299; the modal window does not render in previews, so the sheet body is drawn in place. */
@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenPhotoSheetPreview() {
    GotestTheme {
        Box {
            ProfilePreviewScreen(ProfileUiState.Success(profile = AccountPreviewData.profile, daysRemaining = 30))
            Box(modifier = Modifier.fillMaxSize().background(GotestTheme.colors.scrim))
            ProfilePhotoSheetContent(
                currentPhoto = null,
                state = PhotoSheetUiState(),
                onTakePhoto = {},
                onPickFromGallery = {},
                onSave = {},
                onClose = {},
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .background(GotestTheme.colors.bgApp, GotestTheme.shapes.bottomSheet)
                        .padding(top = GotestTheme.spacing.xl),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun ProfileScreenSignOutPreview() {
    GotestTheme {
        Box {
            ProfilePreviewScreen(ProfileUiState.Success(profile = AccountPreviewData.profile, daysRemaining = 30))
            Box(
                modifier = Modifier.fillMaxSize().background(GotestTheme.colors.scrim),
                contentAlignment = Alignment.Center,
            ) {
                GotestDialogContent(
                    title = stringResource(R.string.account_sign_out_title),
                    message = stringResource(R.string.account_sign_out_message),
                    confirmText = stringResource(R.string.account_sign_out_confirm),
                    onConfirm = {},
                    icon = GotestIcons.Linear.Logout,
                    iconColors = signOutIconColors(),
                    dismissText = stringResource(R.string.account_cancel),
                    modifier = Modifier.padding(GotestTheme.spacing.xl),
                )
            }
        }
    }
}
