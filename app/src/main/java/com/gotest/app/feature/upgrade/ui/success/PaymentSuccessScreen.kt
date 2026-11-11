package com.gotest.app.feature.upgrade.ui.success

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.upgrade.ui.components.UpgradeBottomBar
import com.gotest.app.feature.upgrade.ui.components.UpgradeDimens
import com.gotest.app.feature.upgrade.ui.components.UpgradeInfoRow
import com.gotest.app.feature.upgrade.ui.components.UpgradeProductRow
import com.gotest.app.feature.upgrade.ui.components.UpgradeSectionTitle
import java.time.LocalDate

@Composable
fun PaymentSuccessRoute(
    onOpenDashboard: () -> Unit,
    onOpenTransaction: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentSuccessViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentSuccessScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenDashboard = onOpenDashboard,
        onOpenTransaction = onOpenTransaction,
        modifier = modifier,
    )
}

/** "Upgrade / Pembayaran Berhasil" (frame 11753). */
@Composable
fun PaymentSuccessScreen(
    uiState: PaymentSuccessUiState,
    onEvent: (PaymentSuccessUiEvent) -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (uiState is PaymentSuccessUiState.Success) {
                UpgradeBottomBar(showTopBorder = false) {
                    GotestButton(
                        text = stringResource(R.string.upgrade_success_dashboard),
                        onClick = onOpenDashboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GotestButton(
                        text = stringResource(R.string.upgrade_success_transaction),
                        onClick = onOpenTransaction,
                        modifier = Modifier.fillMaxWidth(),
                        variant = GotestButtonVariant.Plain,
                    )
                }
            }
        },
        containerColor = GotestTheme.colors.bgApp,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val contentModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        when (uiState) {
            PaymentSuccessUiState.Loading -> {
                GotestLoadingView(modifier = contentModifier)
            }

            PaymentSuccessUiState.Error -> {
                GotestErrorView(onRetry = { onEvent(PaymentSuccessUiEvent.Retry) }, modifier = contentModifier)
            }

            is PaymentSuccessUiState.Success -> {
                PaymentSuccessContent(state = uiState, modifier = contentModifier)
            }
        }
    }
}

@Composable
private fun PaymentSuccessContent(
    state: PaymentSuccessUiState.Success,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.screenHorizontal,
                    top = spacing.xxxl,
                    end = spacing.screenHorizontal,
                    bottom = spacing.lg,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SuccessBadge()
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.upgrade_success_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = GotestTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.upgrade_success_message),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        GotestCard(modifier = Modifier.fillMaxWidth(), shape = GotestTheme.shapes.largeIncreased) {
            UpgradeProductRow(
                title = state.productName,
                subtitle = state.productDetail,
                iconContainerColor = GotestTheme.colors.bgYellow,
                iconTint = GotestTheme.colors.bgYellowSoft,
                trailing = {
                    GotestChip(text = stringResource(R.string.upgrade_success_active), tone = GotestChipTone.Green)
                },
            )
        }
        SubscriptionDetails(state = state)
    }
}

/** Green check in two concentric circles. */
@Composable
private fun SuccessBadge(modifier: Modifier = Modifier) {
    val colors = GotestTheme.colors
    Box(
        modifier =
            modifier
                .size(UpgradeDimens.successBadgeOuter)
                .background(colors.bgGreenSoft, GotestTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(UpgradeDimens.successBadgeInner)
                    .background(colors.bgGreen, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Bold.TickCircle),
                contentDescription = null,
                modifier = Modifier.size(UpgradeDimens.successBadgeIcon),
                tint = colors.bgApp,
            )
        }
    }
}

@Composable
private fun SubscriptionDetails(
    state: PaymentSuccessUiState.Success,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val unavailable = stringResource(R.string.upgrade_value_unavailable)
    val activatedOn = state.activatedOn
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        UpgradeSectionTitle(text = stringResource(R.string.upgrade_success_details_title))
        GotestCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
            verticalSpacing = spacing.xl,
        ) {
            UpgradeInfoRow(
                label = stringResource(R.string.upgrade_success_plan_label),
                value = stringResource(R.string.upgrade_success_plan_value, state.productName),
            )
            UpgradeInfoRow(
                label = stringResource(R.string.upgrade_success_activated_label),
                value = activatedOn?.let(GotestFormat::longDate) ?: unavailable,
            )
            UpgradeInfoRow(
                label = stringResource(R.string.upgrade_success_period_label),
                value =
                    stringResource(
                        R.string.upgrade_success_period_value,
                        activatedOn?.let(GotestFormat::mediumDate) ?: unavailable,
                        GotestFormat.mediumDate(state.activeUntil),
                    ),
            )
            UpgradeInfoRow(
                label = stringResource(R.string.upgrade_success_method_label),
                value = state.paymentMethod ?: unavailable,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentSuccessScreenPreview() {
    GotestTheme {
        PaymentSuccessScreen(
            uiState =
                PaymentSuccessUiState.Success(
                    productName = "GoTest Premium",
                    productDetail = "Langganan Tahunan • 12 Bulan",
                    activatedOn = LocalDate.of(2026, 10, 21),
                    activeUntil = LocalDate.of(2027, 10, 21),
                    paymentMethod = "Virtual Account BCA",
                ),
            onEvent = {},
            onOpenDashboard = {},
            onOpenTransaction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentSuccessScreenLoadingPreview() {
    GotestTheme {
        PaymentSuccessScreen(
            uiState = PaymentSuccessUiState.Loading,
            onEvent = {},
            onOpenDashboard = {},
            onOpenTransaction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PaymentSuccessScreenErrorPreview() {
    GotestTheme {
        PaymentSuccessScreen(
            uiState = PaymentSuccessUiState.Error,
            onEvent = {},
            onOpenDashboard = {},
            onOpenTransaction = {},
        )
    }
}
