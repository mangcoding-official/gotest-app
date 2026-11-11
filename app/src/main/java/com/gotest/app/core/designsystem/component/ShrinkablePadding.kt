package com.gotest.app.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Horizontal padding that gives way before its content is squeezed: it starts at [max] and
 * shrinks towards [min] when the content does not fit. The design is drawn on a 440dp frame,
 * while most Android phones are 360–412dp wide, so labels keep the design's padding where
 * there is room and still stay on one line where there is not.
 */
internal fun Modifier.shrinkableHorizontalPadding(
    max: Dp,
    min: Dp = 0.dp,
): Modifier =
    layout { measurable, constraints ->
        val maxPx = max.roundToPx()
        val side =
            if (constraints.hasBoundedWidth) {
                val spare = constraints.maxWidth - measurable.maxIntrinsicWidth(constraints.maxHeight)
                (spare / 2).coerceIn(min.roundToPx(), maxPx)
            } else {
                maxPx
            }
        val placeable = measurable.measure(constraints.offset(horizontal = -2 * side))
        layout(
            constraints.constrainWidth(placeable.width + 2 * side),
            constraints.constrainHeight(placeable.height),
        ) {
            placeable.placeRelative(side, 0)
        }
    }
