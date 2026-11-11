package com.gotest.app.feature.upgrade.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EWallet
import com.gotest.app.feature.upgrade.ui.components.EWalletBadge
import com.gotest.app.feature.upgrade.ui.components.UpgradeDimens
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData
import com.gotest.app.feature.upgrade.ui.components.nameRes

private const val PREVIEW_REMAINING_SECONDS = 587L

/**
 * Body of "Upgrade / Menunggu Pembayaran E-Wallet" (frame 23705): amount, "Lanjutkan di aplikasi
 * <wallet>" with "Buka <wallet> Lagi", and the "Tidak terbuka otomatis?" steps.
 */
@Composable
internal fun EWalletPaymentContent(
    state: PaymentPendingUiState.Success,
    wallet: EWallet,
    onOpenWallet: (EWallet) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val walletName = stringResource(wallet.nameRes)
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        PaymentAmountCard(state = state)
        WalletCard(
            wallet = wallet,
            walletName = walletName,
            isExpired = state.isExpired,
            onOpenWallet = { onOpenWallet(wallet) },
        )
        PaymentInstructionsCard(
            title = stringResource(R.string.upgrade_wallet_help_title),
            steps =
                listOf(
                    stringResource(R.string.upgrade_wallet_help_step_1, walletName),
                    stringResource(R.string.upgrade_wallet_help_step_2, walletName),
                    stringResource(R.string.upgrade_wallet_help_step_3),
                ),
        )
    }
}

@Composable
private fun WalletCard(
    wallet: EWallet,
    walletName: String,
    isExpired: Boolean,
    onOpenWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(UpgradeDimens.paymentCardPadding)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EWalletBadge(wallet = wallet, large = true)
            Text(
                text = stringResource(R.string.upgrade_wallet_continue_title, walletName),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.upgrade_wallet_continue_message, walletName),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            GotestButton(
                text = stringResource(R.string.upgrade_wallet_open_again, walletName),
                onClick = onOpenWallet,
                modifier = Modifier.fillMaxWidth(),
                variant = GotestButtonVariant.Plain,
                size = GotestButtonSize.Medium,
                enabled = !isExpired,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun EWalletPaymentContentPreview() {
    GotestTheme {
        EWalletPaymentContent(
            state =
                PaymentPendingUiState.Success(
                    order = UpgradePreviewData.eWalletOrder,
                    remainingSeconds = PREVIEW_REMAINING_SECONDS,
                    isConfirming = false,
                ),
            wallet = EWallet.GoPay,
            onOpenWallet = {},
        )
    }
}
