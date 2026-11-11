package com.gotest.app.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** "spacing" variables from the design's "Layout" collection, plus the screen gutter. */
@Immutable
data class GotestSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    /** Horizontal padding of app bars and screen content. */
    val screenHorizontal: Dp = 20.dp,
)

/** Fixed component sizes measured from the design's component page. */
@Immutable
data class GotestSizes(
    val strokeThin: Dp = 1.dp,
    val iconXs: Dp = 14.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    val iconXl: Dp = 28.dp,
    val iconButton: Dp = 40.dp,
    val buttonLarge: Dp = 44.dp,
    val buttonMedium: Dp = 36.dp,
    val buttonSmall: Dp = 32.dp,
    val inputHeight: Dp = 44.dp,
    val appBarHeight: Dp = 56.dp,
    val segmentedControlHeight: Dp = 46.dp,
    val segmentHeight: Dp = 36.dp,
    val toggleWidth: Dp = 44.dp,
    val toggleHeight: Dp = 26.dp,
    val toggleKnob: Dp = 20.dp,
    val toggleKnobPadding: Dp = 3.dp,
    val navBarItem: Dp = 52.dp,
    val navBarFab: Dp = 60.dp,
    /** Height of the floating bottom bar above the system navigation bar; tab content pads by it. */
    val bottomBarHeight: Dp = 76.dp,
    val questionCell: Dp = 52.dp,
    val badgeDot: Dp = 8.dp,
    val focusRingWidth: Dp = 3.dp,
    val sheetHandleWidth: Dp = 40.dp,
    val sheetHandleHeight: Dp = 4.dp,
    val dialogIcon: Dp = 56.dp,
    val emptyStateIcon: Dp = 72.dp,
    val progressBarHeight: Dp = 8.dp,
    val avatarXs: Dp = 24.dp,
    val avatarSm: Dp = 32.dp,
    val avatarMd: Dp = 44.dp,
    val avatarLg: Dp = 64.dp,
    val avatarXl: Dp = 88.dp,
    val elevationShadow: Dp = 4.dp,
)

internal val LocalGotestSpacing = staticCompositionLocalOf { GotestSpacing() }
internal val LocalGotestSizes = staticCompositionLocalOf { GotestSizes() }
