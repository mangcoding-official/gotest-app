package com.gotest.app.feature.upgrade.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.EWallet

/** Brand colours of the wallet logos; the design draws them as coloured text badges. */
private val GoPayGreen = Color(0xFF00AA13)
private val OvoPurple = Color(0xFF4C3494)
private val DanaBlue = Color(0xFF118EEA)
private val ShopeePayOrange = Color(0xFFEE4D2D)

/** "GoPay", "OVO", … */
@get:StringRes
internal val EWallet.nameRes: Int
    get() =
        when (this) {
            EWallet.GoPay -> R.string.upgrade_wallet_gopay
            EWallet.Ovo -> R.string.upgrade_wallet_ovo
            EWallet.Dana -> R.string.upgrade_wallet_dana
            EWallet.ShopeePay -> R.string.upgrade_wallet_shopeepay
        }

/** Second line of the wallet's row in "Pilih E-Wallet". */
@get:StringRes
internal val EWallet.descriptionRes: Int
    get() =
        when (this) {
            EWallet.GoPay -> R.string.upgrade_wallet_gopay_description
            EWallet.Ovo -> R.string.upgrade_wallet_ovo_description
            EWallet.Dana -> R.string.upgrade_wallet_dana_description
            EWallet.ShopeePay -> R.string.upgrade_wallet_shopeepay_description
        }

/** Text of the logo badge. Brand marks, so not translated. */
private val EWallet.mark: String
    get() =
        when (this) {
            EWallet.GoPay -> "GP"
            EWallet.Ovo -> "OVO"
            EWallet.Dana -> "DANA"
            EWallet.ShopeePay -> "SP"
        }

private val EWallet.brandColor: Color
    get() =
        when (this) {
            EWallet.GoPay -> GoPayGreen
            EWallet.Ovo -> OvoPurple
            EWallet.Dana -> DanaBlue
            EWallet.ShopeePay -> ShopeePayOrange
        }

/**
 * The wallet's logo: its mark in bold white on the brand colour. 40dp in the sheet rows,
 * [large] (64dp) on the e-wallet payment card. Decorative; the wallet name is always next to it.
 */
@Composable
internal fun EWalletBadge(
    wallet: EWallet,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val size: Dp = if (large) UpgradeDimens.walletBadgeLarge else UpgradeDimens.walletBadge
    val shape = if (large) MaterialTheme.shapes.large else RoundedCornerShape(UpgradeDimens.walletBadgeCorner)
    Box(
        modifier = modifier.size(size).background(wallet.brandColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = wallet.mark,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GotestTheme.colors.bgApp,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EWalletBadgePreview() {
    GotestTheme {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EWallet.entries.forEach { EWalletBadge(wallet = it) }
            EWalletBadge(wallet = EWallet.GoPay, large = true)
        }
    }
}
