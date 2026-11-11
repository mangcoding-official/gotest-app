package com.gotest.app.feature.account.ui.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestEmptyView
import com.gotest.app.core.designsystem.component.GotestErrorView
import com.gotest.app.core.designsystem.component.GotestLoadingView
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.account.ui.components.AccountDetailScaffold
import com.gotest.app.feature.account.ui.components.AccountIconTile
import com.gotest.app.feature.account.ui.components.AccountListCard
import com.gotest.app.feature.account.ui.components.AccountPreviewData
import com.gotest.app.feature.account.ui.components.AccountSectionTitle
import com.gotest.app.feature.account.ui.components.AccountTileSize

@Composable
fun SubscriptionRoute(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onUpgrade = onUpgrade,
        modifier = modifier,
    )
}

@Composable
fun SubscriptionScreen(
    uiState: SubscriptionUiState,
    onEvent: (SubscriptionUiEvent) -> Unit,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountDetailScaffold(
        title = stringResource(R.string.account_subscription_title),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        when (uiState) {
            SubscriptionUiState.Loading -> {
                GotestLoadingView(modifier = Modifier.padding(padding))
            }

            SubscriptionUiState.Error -> {
                GotestErrorView(
                    onRetry = { onEvent(SubscriptionUiEvent.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }

            is SubscriptionUiState.Success -> {
                SubscriptionContent(state = uiState, onUpgrade = onUpgrade, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun SubscriptionContent(
    state: SubscriptionUiState.Success,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.screenHorizontal,
                    top = spacing.sm,
                    bottom = spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        PlanCard(subscription = state.subscription, isActive = state.isActive, onUpgrade = onUpgrade)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AccountSectionTitle(title = stringResource(R.string.account_subscription_details))
            SubscriptionDetails(subscription = state.subscription)
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AccountSectionTitle(title = stringResource(R.string.account_subscription_history))
            PaymentHistory(payments = state.payments)
        }
    }
}

/**
 * "plan": award icon, plan name, status chip and the end date. Premium gets "Perpanjang
 * Langganan"; the Free Trial gets a filled "Upgrade" call to action instead.
 */
@Composable
private fun PlanCard(
    subscription: Subscription,
    isActive: Boolean,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), shape = GotestTheme.shapes.largeIncreased) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Bold.Award),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = colors.bgYellow,
            )
            Text(
                text = subscription.planName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            GotestChip(
                text = stringResource(if (isActive) R.string.account_chip_active else R.string.account_chip_expired),
                tone = if (isActive) GotestChipTone.Green else GotestChipTone.Red,
            )
        }
        Text(
            text = stringResource(R.string.account_active_until, GotestFormat.longDate(subscription.activeUntil)),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
        if (subscription.isPremium) {
            GotestButton(
                text = stringResource(R.string.account_subscription_renew),
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Medium,
            )
        } else {
            GotestButton(
                text = stringResource(R.string.account_subscription_upgrade),
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                size = GotestButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun SubscriptionDetails(
    subscription: Subscription,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.account_subscription_not_available)
    val activatedOn = subscription.activatedOn
    AccountListCard(modifier = modifier) {
        DetailRow(label = stringResource(R.string.account_subscription_plan), value = subscription.planName)
        DetailRow(
            label = stringResource(R.string.account_subscription_activated),
            value = activatedOn?.let(GotestFormat::longDate) ?: none,
        )
        DetailRow(
            label = stringResource(R.string.account_subscription_period),
            value =
                stringResource(
                    R.string.account_subscription_period_value,
                    activatedOn?.let(GotestFormat::mediumDate) ?: none,
                    GotestFormat.mediumDate(subscription.activeUntil),
                ),
        )
        DetailRow(
            label = stringResource(R.string.account_subscription_method),
            value = subscription.paymentMethod ?: none,
        )
    }
}

/** Key/value row: muted caption on the left, value right-aligned. */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun PaymentHistory(
    payments: List<PaymentRecord>,
    modifier: Modifier = Modifier,
) {
    AccountListCard(modifier = modifier) {
        if (payments.isEmpty()) {
            GotestEmptyView(
                icon = GotestIcons.Linear.Note2,
                title = stringResource(R.string.account_subscription_history_empty_title),
                message = stringResource(R.string.account_subscription_history_empty_message),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            payments.forEach { payment -> PaymentRow(payment = payment) }
        }
    }
}

@Composable
private fun PaymentRow(
    payment: PaymentRecord,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val statusColor = if (payment.succeeded) colors.statusSuccess else colors.statusDanger
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountIconTile(
            icon = if (payment.succeeded) GotestIcons.Bold.TickCircle else GotestIcons.Bold.CloseCircle,
            tint = if (payment.succeeded) colors.bgGreen else colors.statusDanger,
            containerColor = if (payment.succeeded) colors.bgGreenSoft else colors.statusDangerSoft,
            size = AccountTileSize.Compact,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(text = payment.planName, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Text(
                text =
                    stringResource(
                        R.string.account_subscription_history_meta,
                        GotestFormat.mediumDate(payment.paidOn),
                        payment.methodLabel,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = GotestFormat.rupiah(payment.amountRupiah),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
            )
            val statusRes =
                if (payment.succeeded) R.string.account_subscription_paid else R.string.account_subscription_failed
            Text(
                text = stringResource(statusRes),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun SubscriptionPreviewScreen(uiState: SubscriptionUiState) {
    SubscriptionScreen(uiState = uiState, onEvent = {}, onBack = {}, onUpgrade = {})
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubscriptionScreenPremiumPreview() {
    GotestTheme {
        SubscriptionPreviewScreen(
            SubscriptionUiState.Success(
                subscription = AccountPreviewData.premiumSubscription,
                isActive = true,
                payments = AccountPreviewData.payments,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubscriptionScreenTrialPreview() {
    GotestTheme {
        SubscriptionPreviewScreen(
            SubscriptionUiState.Success(
                subscription = AccountPreviewData.trialSubscription,
                isActive = true,
                payments = emptyList(),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubscriptionScreenLoadingPreview() {
    GotestTheme { SubscriptionPreviewScreen(SubscriptionUiState.Loading) }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun SubscriptionScreenErrorPreview() {
    GotestTheme { SubscriptionPreviewScreen(SubscriptionUiState.Error) }
}
