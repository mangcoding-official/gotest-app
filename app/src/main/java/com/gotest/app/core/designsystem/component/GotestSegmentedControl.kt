package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Equal-width tab switcher ("Mobile/Segmented Control"). */
@Composable
fun GotestSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(spacing.xs)
                    .selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(GotestTheme.sizes.segmentHeight)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (selected) colors.bgBrandDark else Color.Transparent)
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelect(index) },
                            ).padding(horizontal = spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.textOnBrand else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestSegmentedControlPreview() {
    GotestTheme {
        GotestSegmentedControl(
            options = listOf("Paket Latihan", "Paket Tryout"),
            selectedIndex = 0,
            onSelect = {},
            modifier = Modifier.padding(GotestTheme.spacing.lg),
        )
    }
}
