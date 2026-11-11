package com.gotest.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.gotest.app.core.designsystem.theme.GotestTheme

/** The design's "Mobile/Toggle" switch (44×26, brand fill when on). */
@Composable
fun GotestToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GotestTheme.colors
    val sizes = GotestTheme.sizes
    val spacing = GotestTheme.spacing
    val track by animateColorAsState(
        targetValue = if (checked) colors.bgBrandDark else colors.bgGrey,
        label = "toggleTrack",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) sizes.toggleWidth - sizes.toggleKnob - sizes.toggleKnobPadding * 2 else spacing.none,
        label = "toggleKnob",
    )
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(sizes.toggleWidth, sizes.toggleHeight)
                    .background(track, GotestTheme.shapes.full)
                    .padding(sizes.toggleKnobPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier =
                    Modifier
                        .offset { IntOffset(knobOffset.roundToPx(), 0) }
                        .size(sizes.toggleKnob)
                        .shadow(sizes.strokeThin, GotestTheme.shapes.full)
                        .background(colors.bgApp, GotestTheme.shapes.full),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestTogglePreview() {
    GotestTheme {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            GotestToggle(checked = true, onCheckedChange = {})
            GotestToggle(checked = false, onCheckedChange = {})
        }
    }
}
