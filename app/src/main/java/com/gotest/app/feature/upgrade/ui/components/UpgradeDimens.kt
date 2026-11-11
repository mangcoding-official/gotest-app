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

    /** The selected method card is violet/50, half of bg/brand-soft over white. */
    const val SELECTED_METHOD_CONTAINER_ALPHA = 0.5f
}
