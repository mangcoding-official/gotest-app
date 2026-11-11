package com.gotest.app.feature.upgrade.ui.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.upgrade.ui.components.UpgradeDimens
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData
import com.gotest.app.feature.upgrade.ui.components.UpgradeSectionTitle

private const val PREVIEW_REMAINING_SECONDS = 892L

/**
 * White amount card of the QRIS and e-wallet screens: "Menunggu Pembayaran", the total and the
 * "Bayar dalam mm:ss" chip, which turns red once the payment window has closed.
 */
@Composable
internal fun PaymentAmountCard(
    state: PaymentPendingUiState.Success,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(UpgradeDimens.compactCardGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.upgrade_payment_waiting),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
            Text(
                text = GotestFormat.rupiah(state.order.totalRupiah),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            if (state.isExpired) {
                GotestChip(
                    text = stringResource(R.string.upgrade_payment_time_up),
                    tone = GotestChipTone.Red,
                    leadingIcon = GotestIcons.Linear.Calendar,
                )
            } else {
                GotestChip(
                    text =
                        stringResource(
                            R.string.upgrade_payment_pay_within,
                            GotestFormat.clock(state.remainingSeconds.toInt()),
                        ),
                    tone = GotestChipTone.Yellow,
                    leadingIcon = GotestIcons.Linear.Calendar,
                )
            }
        }
    }
}

/** A titled card of numbered steps: "Cara Membayar" (QRIS) and "Tidak terbuka otomatis?" (e-wallet). */
@Composable
internal fun PaymentInstructionsCard(
    title: String,
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = UpgradeDimens.instructionGap) {
        UpgradeSectionTitle(text = title)
        steps.forEachIndexed { index, step -> InstructionStep(number = index + 1, text = step) }
    }
}

@Composable
private fun InstructionStep(
    number: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UpgradeDimens.instructionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(UpgradeDimens.instructionBadge)
                    .background(GotestTheme.colors.bgBrandSoft, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = GotestFormat.number(number),
                style = GotestTheme.typography.labelCompact,
                color = GotestTheme.colors.textBrandDark,
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentPendingComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            PaymentAmountCard(
                state =
                    PaymentPendingUiState.Success(
                        order = UpgradePreviewData.qrisOrder,
                        remainingSeconds = PREVIEW_REMAINING_SECONDS,
                        isConfirming = false,
                    ),
            )
            PaymentAmountCard(
                state =
                    PaymentPendingUiState.Success(
                        order = UpgradePreviewData.qrisOrder,
                        remainingSeconds = 0,
                        isConfirming = false,
                    ),
            )
            PaymentInstructionsCard(
                title = "Cara Membayar",
                steps = listOf("Buka aplikasi bank atau e-wallet yang mendukung QRIS.", "Pindai kode QR di atas."),
            )
        }
    }
}
