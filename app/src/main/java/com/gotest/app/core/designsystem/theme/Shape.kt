package com.gotest.app.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// radius/* variables from the design's "Layout" collection.
private val RadiusXs = 4.dp
private val RadiusSm = 8.dp
private val RadiusMd = 12.dp
private val RadiusLg = 16.dp
private val RadiusXl = 20.dp
private val RadiusXxl = 24.dp

/** radius/xs, sm, md, lg, xxl. */
internal val GotestMaterialShapes =
    Shapes(
        extraSmall = RoundedCornerShape(RadiusXs),
        small = RoundedCornerShape(RadiusSm),
        medium = RoundedCornerShape(RadiusMd),
        large = RoundedCornerShape(RadiusLg),
        extraLarge = RoundedCornerShape(RadiusXxl),
    )

/** Radii without a Material role. */
@Immutable
data class GotestShapes(
    /** radius/xl, hero cards. */
    val largeIncreased: Shape,
    /** radius/xxl on the top corners only, bottom sheets. */
    val bottomSheet: Shape,
    /** radius/full, pills and avatars. */
    val full: Shape,
)

internal val DefaultGotestShapes =
    GotestShapes(
        largeIncreased = RoundedCornerShape(RadiusXl),
        bottomSheet = RoundedCornerShape(topStart = RadiusXxl, topEnd = RadiusXxl),
        full = CircleShape,
    )

internal val LocalGotestShapes = staticCompositionLocalOf { DefaultGotestShapes }
