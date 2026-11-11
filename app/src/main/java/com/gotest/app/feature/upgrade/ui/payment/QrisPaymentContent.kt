package com.gotest.app.feature.upgrade.ui.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.QrisCode
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.upgrade.ui.components.UpgradeDimens
import com.gotest.app.feature.upgrade.ui.components.UpgradePreviewData

/** An expired code stays visible but faded, so nobody scans it. */
private const val EXPIRED_QR_ALPHA = 0.2f
private const val PREVIEW_REMAINING_SECONDS = 892L

/** Body of "Upgrade / Bayar QRIS" (frame 23375): amount, the QR card and "Cara Membayar". */
@Composable
internal fun QrisPaymentContent(
    state: PaymentPendingUiState.Success,
    qris: QrisCode,
    onSaveQr: () -> Unit,
    onShareQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val amount = GotestFormat.rupiah(state.order.totalRupiah)
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        PaymentAmountCard(state = state)
        QrisCard(
            qris = qris,
            amount = amount,
            isExpired = state.isExpired,
            onSaveQr = onSaveQr,
            onShareQr = onShareQr,
        )
        PaymentInstructionsCard(
            title = stringResource(R.string.upgrade_payment_steps_title),
            steps =
                listOf(
                    stringResource(R.string.upgrade_qris_step_1),
                    stringResource(R.string.upgrade_qris_step_2),
                    stringResource(R.string.upgrade_qris_step_3, amount),
                ),
        )
    }
}

/** Merchant, the QR code, NMID and validity, "Simpan QR" and "Bagikan". */
@Composable
private fun QrisCard(
    qris: QrisCode,
    amount: String,
    isExpired: Boolean,
    onSaveQr: () -> Unit,
    onShareQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(UpgradeDimens.paymentCardPadding)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QrisBadge()
                Text(
                    text = qris.merchantName,
                    style = MaterialTheme.typography.labelLarge,
                    color = GotestTheme.colors.textPrimary,
                )
            }
            QrCodeFrame(
                payload = qris.payload,
                contentDescription = stringResource(R.string.upgrade_qris_cd_code, amount),
                isExpired = isExpired,
            )
            Text(
                text = stringResource(R.string.upgrade_qris_merchant_info, qris.nmid),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                GotestButton(
                    text = stringResource(R.string.upgrade_qris_save),
                    onClick = onSaveQr,
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                    size = GotestButtonSize.Medium,
                    enabled = !isExpired,
                )
                GotestButton(
                    text = stringResource(R.string.upgrade_qris_share),
                    onClick = onShareQr,
                    modifier = Modifier.weight(1f),
                    variant = GotestButtonVariant.Plain,
                    size = GotestButtonSize.Medium,
                    enabled = !isExpired,
                )
            }
        }
    }
}

/** The red "QRIS" logo badge. */
@Composable
private fun QrisBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.upgrade_qris_badge),
        modifier =
            modifier
                .background(GotestTheme.colors.statusDanger, MaterialTheme.shapes.extraSmall)
                .padding(horizontal = GotestTheme.spacing.sm, vertical = UpgradeDimens.qrisBadgeVerticalPadding),
        style = GotestTheme.typography.labelCompact,
        fontWeight = FontWeight.Bold,
        color = GotestTheme.colors.bgApp,
    )
}

/**
 * The code in its white frame, rendered from [payload]. One pixel per module, scaled up without
 * filtering so the modules stay sharp.
 */
@Composable
private fun QrCodeFrame(
    payload: String,
    contentDescription: String,
    isExpired: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val code = remember(payload) { runCatching { QrCodeEncoder.screenBitmap(payload).asImageBitmap() }.getOrNull() }
    Box(
        modifier =
            modifier
                .size(UpgradeDimens.qrFrame)
                .background(GotestTheme.colors.bgApp, shape)
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (code != null) {
            Image(
                bitmap = code,
                contentDescription = contentDescription,
                modifier =
                    Modifier
                        .size(UpgradeDimens.qrCode)
                        .alpha(if (isExpired) EXPIRED_QR_ALPHA else 1f),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun QrisPaymentContentPreview() {
    GotestTheme {
        val order = UpgradePreviewData.qrisOrder
        QrisPaymentContent(
            state =
                PaymentPendingUiState.Success(
                    order = order,
                    remainingSeconds = PREVIEW_REMAINING_SECONDS,
                    isConfirming = false,
                ),
            qris = checkNotNull(order.qris),
            onSaveQr = {},
            onShareQr = {},
        )
    }
}
