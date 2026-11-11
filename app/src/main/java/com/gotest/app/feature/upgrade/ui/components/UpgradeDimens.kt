package com.gotest.app.feature.upgrade.ui.components

import androidx.compose.ui.unit.dp

/** Sizes from the upgrade frames that the shared tokens do not cover. */
internal object UpgradeDimens {
    /** Brand border of the selected plan card. */
    val selectedPlanBorder = 2.dp

    /** Focus border of the selected payment method card. */
    val selectedMethodBorder = 1.5.dp

    val methodCardPadding = 14.dp
    val methodIconBox = 36.dp
    val methodIcon = 18.dp

    val radio = 20.dp
    val radioSelectedStroke = 6.dp
    val radioStroke = 1.5.dp

    /** Icon tile in front of "GoTest Premium". */
    val productIconBox = 40.dp

    val bankBadgeWidth = 44.dp
    val bankBadgeHeight = 32.dp
    val stepBadge = 24.dp

    val successBadgeOuter = 88.dp
    val successBadgeInner = 60.dp
    val successBadgeIcon = 32.dp

    /** 6dp gap of the promo headline and terms cards and of the QRIS/e-wallet amount card. */
    val compactCardGap = 6.dp
    val promoBenefitGap = 10.dp
    val promoBenefitIcon = 18.dp
    val discountBadgeHorizontalPadding = 10.dp
    val discountBadgeVerticalPadding = 6.dp

    /** White cards of the QRIS and e-wallet payment screens. */
    val paymentCardPadding = 20.dp

    /** White frame around the QR code, and the code inside it. */
    val qrFrame = 220.dp
    val qrCode = 196.dp
    val qrisBadgeVerticalPadding = 3.dp

    /** Numbered steps of the QRIS and e-wallet instructions. */
    val instructionBadge = 20.dp
    val instructionGap = 10.dp

    /** E-wallet logo badges: the sheet rows and the e-wallet payment card. */
    val walletBadge = 40.dp
    val walletBadgeCorner = 10.dp
    val walletBadgeLarge = 64.dp

    /** The promo banner artwork is 400 x 68 in the design. */
    const val PROMO_BANNER_ASPECT_RATIO = 400f / 68f

    /** Secondary text of the yellow promo price card: bg/yellow-dark at 50%. */
    const val PROMO_MUTED_ALPHA = 0.5f

    /** The selected method card is violet/50, half of bg/brand-soft over white. */
    const val SELECTED_METHOD_CONTAINER_ALPHA = 0.5f
}
