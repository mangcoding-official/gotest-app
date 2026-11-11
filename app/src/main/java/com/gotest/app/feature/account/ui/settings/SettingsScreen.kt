package com.gotest.app.feature.account.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestToggle
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.account.domain.SettingSwitch
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountNotice
import com.gotest.app.feature.account.ui.components.AccountNoticeEffect
import com.gotest.app.feature.account.ui.components.AccountPreviewData
import com.gotest.app.feature.account.ui.components.AccountRowArrow
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.mainlevel.titleRes
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Navigation out of Pengaturan, grouped so the screen signature stays readable. */
data class SettingsNavigation(
    val onOpenMainLevel: () -> Unit,
    val onOpenSubscription: () -> Unit,
    val onOpenPersonalData: () -> Unit,
    val onOpenDownloadData: () -> Unit,
)

/** [notice] reports Jenjang Utama and Unduh Data Akun results. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    navigation: SettingsNavigation,
    notice: AccountNotice?,
    onNoticeConsume: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    AccountNoticeEffect(notice = notice, onConsume = onNoticeConsume, snackbarHostState = snackbarHostState)
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is SettingsEffect.ShowMessage -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    SettingsScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        navigation = navigation,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    navigation: SettingsNavigation,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_settings_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        when (uiState) {
            SettingsUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            SettingsUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(SettingsUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is SettingsUiState.Success -> {
                SettingsContent(
                    state = uiState,
                    onSwitchChange = { switch, enabled -> onEvent(SettingsUiEvent.SwitchChange(switch, enabled)) },
                    navigation = navigation,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Success,
    onSwitchChange: (SettingSwitch, Boolean) -> Unit,
    navigation: SettingsNavigation,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val settings = state.settings
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AccountSectionTitle(
            title = stringResource(R.string.account_settings_notifications),
            subtitle = stringResource(R.string.account_settings_notifications_subtitle),
            modifier = Modifier.padding(top = spacing.sm),
        )
        AccountListCard {
            notificationRows.forEach { row ->
                SwitchSettingRow(
                    title = stringResource(row.titleRes),
                    subtitle = stringResource(row.subtitleRes),
                    checked = settings.isOn(row.switch),
                    onCheckedChange = { onSwitchChange(row.switch, it) },
                )
            }
        }
        AccountSectionTitle(title = stringResource(R.string.account_settings_learning))
        AccountListCard {
            SwitchSettingRow(
                title = stringResource(R.string.account_settings_reminder),
                subtitle =
                    stringResource(
                        R.string.account_settings_reminder_schedule,
                        GotestFormat.time(settings.reminderTime),
                    ),
                checked = settings.isOn(SettingSwitch.StudyReminder),
                onCheckedChange = { onSwitchChange(SettingSwitch.StudyReminder, it) },
            )
            ClickableSettingRow(
                title = stringResource(R.string.account_settings_main_level),
                subtitle = stringResource(settings.mainLevel.titleRes),
                onClick = navigation.onOpenMainLevel,
            )
        }
        SubscriptionSection(state = state, onOpenSubscription = navigation.onOpenSubscription)
        PrivacySection(
            promoEmails = settings.isOn(SettingSwitch.PromoEmails),
            onPromoEmailsChange = { onSwitchChange(SettingSwitch.PromoEmails, it) },
            onOpenPersonalData = navigation.onOpenPersonalData,
            onOpenDownloadData = navigation.onOpenDownloadData,
        )
    }
}

@Composable
private fun SubscriptionSection(
    state: SettingsUiState.Success,
    onOpenSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        AccountSectionTitle(title = stringResource(R.string.account_settings_subscription))
        AccountListCard {
            ClickableSettingRow(
                title = state.subscription.planName,
                subtitle =
                    stringResource(
                        R.string.account_active_until,
                        GotestFormat.mediumDate(state.subscription.activeUntil),
                    ),
                onClick = onOpenSubscription,
                trailing = { SubscriptionStatusChip(active = state.isSubscriptionActive) },
            )
        }
    }
}

@Composable
private fun PrivacySection(
    promoEmails: Boolean,
    onPromoEmailsChange: (Boolean) -> Unit,
    onOpenPersonalData: () -> Unit,
    onOpenDownloadData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
        AccountSectionTitle(title = stringResource(R.string.account_settings_privacy))
        AccountListCard {
            ClickableSettingRow(
                title = stringResource(R.string.account_settings_personal_data),
                subtitle = stringResource(R.string.account_settings_personal_data_subtitle),
                onClick = onOpenPersonalData,
            )
            SwitchSettingRow(
                title = stringResource(R.string.account_settings_promo),
                subtitle = stringResource(R.string.account_settings_promo_subtitle),
                checked = promoEmails,
                onCheckedChange = onPromoEmailsChange,
            )
            ClickableSettingRow(
                title = stringResource(R.string.account_settings_download),
                subtitle = stringResource(R.string.account_settings_download_subtitle),
                onClick = onOpenDownloadData,
            )
        }
    }
}

private data class NotificationRow(
    val switch: SettingSwitch,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
)

private val notificationRows =
    listOf(
        NotificationRow(
            SettingSwitch.ActivityResults,
            R.string.account_settings_activity,
            R.string.account_settings_activity_subtitle,
        ),
        NotificationRow(
            SettingSwitch.RankingUpdates,
            R.string.account_settings_ranking,
            R.string.account_settings_ranking_subtitle,
        ),
        NotificationRow(
            SettingSwitch.NewPackages,
            R.string.account_settings_new_packages,
            R.string.account_settings_new_packages_subtitle,
        ),
        NotificationRow(
            SettingSwitch.SubscriptionStatus,
            R.string.account_settings_subscription_status,
            R.string.account_settings_subscription_status_subtitle,
        ),
    )

/** The design's "setting" row: title, muted subtitle and a trailing control. */
@Composable
private fun SettingRowLayout(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.padding(horizontal = spacing.lg, vertical = spacing.md + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = GotestTheme.colors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        }
        trailing()
    }
}

/** Whole row toggles; the switch itself is hidden from accessibility so it is announced once. */
@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRowLayout(
        title = title,
        subtitle = subtitle,
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        GotestToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun ClickableSettingRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = { AccountRowArrow() },
) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = GotestTheme.colors.bgApp) {
        SettingRowLayout(title = title, subtitle = subtitle, trailing = trailing)
    }
}

@Composable
private fun SubscriptionStatusChip(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    GotestChip(
        text = stringResource(if (active) R.string.account_chip_active else R.string.account_chip_expired),
        modifier = modifier,
        tone = if (active) GotestChipTone.Green else GotestChipTone.Red,
    )
}

private val previewNavigation =
    SettingsNavigation(onOpenMainLevel = {}, onOpenSubscription = {}, onOpenPersonalData = {}, onOpenDownloadData = {})

@Composable
private fun SettingsPreviewScreen(uiState: SettingsUiState) {
    SettingsScreen(uiState = uiState, onEvent = {}, onBack = {}, navigation = previewNavigation)
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SettingsScreenPreview() {
    GotestTheme {
        SettingsPreviewScreen(
            SettingsUiState.Success(
                settings = AccountPreviewData.settings,
                subscription = AccountPreviewData.premiumSubscription,
                isSubscriptionActive = true,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SettingsScreenTrialPreview() {
    GotestTheme {
        SettingsPreviewScreen(
            SettingsUiState.Success(
                settings = AccountPreviewData.settings,
                subscription = AccountPreviewData.trialSubscription,
                isSubscriptionActive = false,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SettingsScreenLoadingPreview() {
    GotestTheme { SettingsPreviewScreen(SettingsUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SettingsScreenErrorPreview() {
    GotestTheme { SettingsPreviewScreen(SettingsUiState.Error) }
}
