package com.gotest.app.feature.upgrade.ui.checkout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBackButton
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.component.GotestTopAppBar
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.upgrade.ui.components.PaymentMethodList
import com.gotest.app.feature.upgrade.ui.components.UpgradeBottomBar
import com.gotest.app.feature.upgrade.ui.components.UpgradeCaption
import com.gotest.app.feature.upgrade.ui.components.UpgradeInfoRow
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData
import com.gotest.app.feature.upgrade.ui.components.UpgradeProductRow
import com.gotest.app.feature.upgrade.ui.components.UpgradeSectionTitle
import kotlinx.coroutines.launch

private const val CHECKOUT_STEP = 1
private const val UPGRADE_STEPS = 2

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPayment: (orderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is CheckoutEffect.OpenPayment -> {
                onOpenPayment(effect.orderId)
            }

            is CheckoutEffect.ShowMessage -> {
                scope.launch { snackbarHostState.showSnackbar(resources.getString(effect.messageRes)) }
            }
        }
    }
    CheckoutScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEditProfile = onEditProfile,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * "Upgrade / Checkout" (frame 11497), step 1 of 2. E-Wallet opens "Pilih E-Wallet" (frame 23508);
 * with a promo the summary strikes through the normal price and adds a "Promo" row with its chip.
 */
@Composable
fun CheckoutScreen(
    uiState: CheckoutUiState,
    onEvent: (CheckoutUiEvent) -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GotestTopAppBar(
                title = stringResource(R.string.upgrade_checkout_title),
                subtitle = stringResource(R.string.upgrade_step_progress, CHECKOUT_STEP, UPGRADE_STEPS),
                navigationIcon = { GotestBackButton(onClick = onBack) },
            )
        },
        bottomBar = {
            if (uiState is CheckoutUiState.Success) {
                CheckoutBottomBar(state = uiState, onPay = { onEvent(CheckoutUiEvent.Pay) })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = GotestTheme.colors.bgSubtle,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (uiState) {
            CheckoutUiState.Loading -> {
                GotestLoadingView(modifier = contentModifier)
            }

            CheckoutUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(CheckoutUiEvent.Retry) }, modifier = contentModifier)
            }

            is CheckoutUiState.Success -> {
                CheckoutContent(
                    state = uiState,
                    onEvent = onEvent,
                    onEditProfile = onEditProfile,
                    modifier = contentModifier,
                )
                if (uiState.isWalletSheetVisible) {
                    EWalletSheet(
                        selected = uiState.selectedWallet,
                        isSubmitting = uiState.isSubmitting,
                        onSelect = { onEvent(CheckoutUiEvent.SelectWallet(it)) },
                        onPay = { onEvent(CheckoutUiEvent.PayWithWallet) },
                        onDismiss = { onEvent(CheckoutUiEvent.DismissWalletSheet) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutContent(
    state: CheckoutUiState.Success,
    onEvent: (CheckoutUiEvent) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    top = spacing.sm,
                    end = spacing.screenHorizontal,
                    bottom = spacing.lg,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        AccountSection(fullName = state.fullName, email = state.email, onEditProfile = onEditProfile)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            UpgradeSectionTitle(text = stringResource(R.string.upgrade_checkout_method_title))
            PaymentMethodList(
                selected = state.selectedMethod,
                onSelect = { onEvent(CheckoutUiEvent.SelectMethod(it)) },
            )
        }
        OrderSummarySection(state = state)
    }
}

@Composable
private fun AccountSection(
    fullName: String,
    email: String,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UpgradeSectionTitle(text = stringResource(R.string.upgrade_checkout_account_title))
            Text(
                text = stringResource(R.string.upgrade_checkout_edit),
                modifier =
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button, onClick = onEditProfile)
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = spacing.xs),
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textBrandDark,
            )
        }
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        ) {
            UpgradeInfoRow(label = stringResource(R.string.upgrade_checkout_name_label), value = fullName)
            UpgradeInfoRow(label = stringResource(R.string.upgrade_checkout_email_label), value = email)
        }
        Text(
            text = stringResource(R.string.upgrade_checkout_account_note),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun OrderSummarySection(
    state: CheckoutUiState.Success,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val rowPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        UpgradeSectionTitle(text = stringResource(R.string.upgrade_checkout_summary_title))
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(),
            verticalSpacing = spacing.none,
        ) {
            UpgradeProductRow(
                title = stringResource(R.string.upgrade_product_premium),
                subtitle = stringResource(R.string.upgrade_product_yearly_detail),
                iconContainerColor = GotestTheme.colors.bgBrandDark,
                iconTint = GotestTheme.colors.bgYellow,
                modifier = Modifier.padding(rowPadding),
            )
            Column(
                modifier = Modifier.padding(rowPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                UpgradeInfoRow(
                    label = stringResource(R.string.upgrade_checkout_fee),
                    value = GotestFormat.rupiah(state.feeRupiah),
                )
                val promo = state.promo
                if (promo == null) {
                    UpgradeInfoRow(
                        label = stringResource(R.string.upgrade_checkout_price),
                        value = GotestFormat.rupiah(state.priceRupiah),
                    )
                } else {
                    PromoPriceRow(originalRupiah = promo.originalPriceRupiah, priceRupiah = state.priceRupiah)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.upgrade_promo_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = GotestTheme.colors.textSecondary,
                        )
                        GotestChip(text = promo.label, tone = GotestChipTone.Red)
                    }
                }
            }
        }
    }
}

/** "Harga Paket" with a promo: the normal price struck through before the promo price, as on the promo card. */
@Composable
private fun PromoPriceRow(
    originalRupiah: Long,
    priceRupiah: Long,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val original = GotestFormat.rupiah(originalRupiah)
    val originalDescription = stringResource(R.string.upgrade_promo_cd_original_price, original)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.upgrade_checkout_price),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        Text(
            text = original,
            modifier = Modifier.semantics { contentDescription = originalDescription },
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            textDecoration = TextDecoration.LineThrough,
        )
        Text(
            text = GotestFormat.rupiah(priceRupiah),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun CheckoutBottomBar(
    state: CheckoutUiState.Success,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UpgradeBottomBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.upgrade_checkout_total),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
            Text(
                text = GotestFormat.rupiah(state.totalRupiah),
                style = MaterialTheme.typography.headlineSmall,
                color = GotestTheme.colors.textPrimary,
            )
        }
        GotestButton(
            text = stringResource(R.string.upgrade_checkout_pay),
            onClick = onPay,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting,
        )
        UpgradeCaption(text = stringResource(R.string.upgrade_checkout_terms))
    }
}

private val previewSuccess =
    CheckoutUiState.Success(
        fullName = "Gilang Permana",
        email = "gilang@gmail.com",
        plan = UpgradePreviewData.premiumPlan,
        promo = null,
        selectedMethod = PaymentMethodType.VirtualAccount,
        selectedWallet = EWallet.GoPay,
        isWalletSheetVisible = false,
        priceRupiah = 100_000,
        feeRupiah = 10_000,
        totalRupiah = 110_000,
        isSubmitting = false,
    )

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CheckoutScreenPreview() {
    GotestTheme {
        CheckoutScreen(
            uiState = previewSuccess,
            onEvent = {},
            onBack = {},
            onEditProfile = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CheckoutScreenSubmittingPreview() {
    GotestTheme {
        CheckoutScreen(
            uiState = previewSuccess.copy(selectedMethod = PaymentMethodType.Qris, isSubmitting = true),
            onEvent = {},
            onBack = {},
            onEditProfile = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CheckoutScreenPromoPreview() {
    GotestTheme {
        CheckoutScreen(
            uiState = previewSuccess.copy(promo = UpgradePreviewData.promo),
            onEvent = {},
            onBack = {},
            onEditProfile = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CheckoutScreenLoadingPreview() {
    GotestTheme {
        CheckoutScreen(
            uiState = CheckoutUiState.Loading,
            onEvent = {},
            onBack = {},
            onEditProfile = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun CheckoutScreenErrorPreview() {
    GotestTheme {
        CheckoutScreen(
            uiState = CheckoutUiState.Error,
            onEvent = {},
            onBack = {},
            onEditProfile = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
