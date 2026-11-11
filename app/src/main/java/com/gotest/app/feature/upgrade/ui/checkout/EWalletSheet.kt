package com.gotest.app.feature.upgrade.ui.checkout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EWallet
import com.gotest.app.feature.upgrade.ui.components.EWalletBadge
import com.gotest.app.feature.upgrade.ui.components.RadioIndicator
import com.gotest.app.feature.upgrade.ui.components.descriptionRes
import com.gotest.app.feature.upgrade.ui.components.nameRes

/** "Pilih E-Wallet" (frame 23508): one radio row per wallet and "Bayar dengan <wallet>". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EWalletSheet(
    selected: EWallet,
    isSubmitting: Boolean,
    onSelect: (EWallet) -> Unit,
    onPay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        EWalletSheetContent(selected = selected, isSubmitting = isSubmitting, onSelect = onSelect, onPay = onPay)
    }
}

@Composable
private fun EWalletSheetContent(
    selected: EWallet,
    isSubmitting: Boolean,
    onSelect: (EWallet) -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.upgrade_wallet_sheet_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.upgrade_wallet_sheet_message),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, MaterialTheme.shapes.large)
                    .padding(spacing.sm)
                    .selectableGroup(),
        ) {
            EWallet.entries.forEach { wallet ->
                EWalletOption(wallet = wallet, selected = wallet == selected, onClick = { onSelect(wallet) })
            }
        }
        GotestButton(
            text = stringResource(R.string.upgrade_wallet_pay, stringResource(selected.nameRes)),
            onClick = onPay,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting,
        )
    }
}

/** A wallet row; the selected one sits on a grey tile with a hairline border. */
@Composable
private fun EWalletOption(
    wallet: EWallet,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.bgSubtle else Color.Transparent,
        border = if (selected) BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle) else null,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EWalletBadge(wallet = wallet)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(
                    text = stringResource(wallet.nameRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(wallet.descriptionRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            RadioIndicator(selected = selected)
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun EWalletSheetContentPreview() {
    GotestTheme {
        EWalletSheetContent(selected = EWallet.GoPay, isSubmitting = false, onSelect = {}, onPay = {})
    }
}
