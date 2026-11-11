package com.gotest.app.feature.account.ui.personaldata

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountDimens
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountNotice
import com.gotest.app.feature.account.ui.components.AccountNoticeEffect
import com.gotest.app.feature.account.ui.components.AccountPreviewData
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.components.openWebLink
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_TAG = "privacy_policy"

/** "Data yang kami simpan" (frame 23170); fixed copy, not per student. */
private val storedData =
    listOf(
        GotestIcons.Linear.TaskSquare to R.string.account_personal_stored_results,
        GotestIcons.Linear.Message to R.string.account_personal_stored_chats,
        GotestIcons.Linear.Bank to R.string.account_personal_stored_transactions,
        GotestIcons.Linear.Lock to R.string.account_personal_stored_devices,
    )

/** [notice] is "Informasi akun berhasil disimpan" after "Ubah" → Edit Informasi Akun saves. */
@Composable
fun PersonalDataRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    notice: AccountNotice?,
    onNoticeConsume: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    AccountNoticeEffect(notice = notice, onConsume = onNoticeConsume, snackbarHostState = snackbarHostState)
    PersonalDataScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEditProfile = onEditProfile,
        onOpenPrivacyPolicy = {
            if (!context.openWebLink(resources.getString(R.string.account_personal_policy_url))) {
                scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.account_personal_no_browser))
                }
            }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** "Akun / Pengaturan – Data Pribadi" (frame 23170). */
@Composable
fun PersonalDataScreen(
    uiState: PersonalDataUiState,
    onEvent: (PersonalDataUiEvent) -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_personal_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        when (uiState) {
            PersonalDataUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            PersonalDataUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(PersonalDataUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is PersonalDataUiState.Success -> {
                PersonalDataContent(
                    state = uiState,
                    onEditProfile = onEditProfile,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    contentPadding = padding,
                )
            }
        }
    }
}

@Composable
private fun PersonalDataContent(
    state: PersonalDataUiState.Success,
    onEditProfile: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AccountInfoHeader(onEditProfile = onEditProfile)
        AccountListCard {
            PersonalDataRow(R.string.account_personal_name, state.fullName)
            PersonalDataRow(R.string.account_personal_email, state.email)
            PersonalDataRow(R.string.account_personal_phone, state.details.phoneNumber)
            PersonalDataRow(R.string.account_personal_birth_date, GotestFormat.longDate(state.details.birthDate))
            PersonalDataRow(R.string.account_personal_school, state.details.school)
            PersonalDataRow(R.string.account_personal_city, state.details.city)
        }
        AccountSectionTitle(
            title = stringResource(R.string.account_personal_stored),
            subtitle = stringResource(R.string.account_personal_stored_subtitle),
        )
        GotestCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
            storedData.forEach { (icon, textRes) -> StoredDataRow(icon = icon, textRes = textRes) }
        }
        PrivacyPolicyLink(onClick = onOpenPrivacyPolicy)
    }
}

/** "Informasi akun" heading with the "Ubah" link to Edit Informasi Akun. */
@Composable
private fun AccountInfoHeader(
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        AccountSectionTitle(
            title = stringResource(R.string.account_personal_info),
            subtitle = stringResource(R.string.account_personal_info_subtitle),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.account_personal_edit),
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(role = Role.Button, onClick = onEditProfile),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.bgBrand,
        )
    }
}

/** "row": a fixed-width muted label and the value. */
@Composable
private fun PersonalDataRow(
    @StringRes labelRes: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.md + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.width(AccountDimens.personalDataLabelWidth),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun StoredDataRow(
    @DrawableRes icon: Int,
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm + GotestTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(AccountDimens.iconSmall),
            tint = GotestTheme.colors.iconDefault,
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

/** "Selengkapnya di Kebijakan Privasi"; only the link part is tappable. */
@Composable
private fun PrivacyPolicyLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyles = TextLinkStyles(style = SpanStyle(color = GotestTheme.colors.bgBrand))
    val prefix = stringResource(R.string.account_personal_policy_prefix)
    val link = stringResource(R.string.account_personal_policy_link)
    val text =
        buildAnnotatedString {
            append(prefix)
            append(' ')
            withLink(LinkAnnotation.Clickable(PRIVACY_POLICY_TAG, linkStyles) { onClick() }) { append(link) }
        }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = GotestTheme.colors.textSecondary,
    )
}

private val previewSuccess =
    PersonalDataUiState.Success(
        fullName = AccountPreviewData.profile.fullName,
        email = "gilang@gmail.com",
        details = AccountPreviewData.personalDetails,
    )

@Composable
private fun PersonalDataPreviewScreen(uiState: PersonalDataUiState) {
    PersonalDataScreen(uiState = uiState, onEvent = {}, onBack = {}, onEditProfile = {}, onOpenPrivacyPolicy = {})
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PersonalDataScreenPreview() {
    GotestTheme { PersonalDataPreviewScreen(previewSuccess) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PersonalDataScreenNarrowPreview() {
    GotestTheme {
        PersonalDataPreviewScreen(
            previewSuccess.copy(
                details = previewSuccess.details.copy(school = "SMA Negeri 3 Bandung Jawa Barat Indonesia"),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PersonalDataScreenLoadingPreview() {
    GotestTheme { PersonalDataPreviewScreen(PersonalDataUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PersonalDataScreenErrorPreview() {
    GotestTheme { PersonalDataPreviewScreen(PersonalDataUiState.Error) }
}
