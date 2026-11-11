package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Rounded filter toggle ("Mobile/Filter Pill"), usually placed in a horizontal list. */
@Composable
fun GotestFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = GotestTheme.shapes.full,
        color = if (selected) colors.bgBrandDark else colors.bgApp,
        contentColor = if (selected) colors.textOnBrand else colors.textPrimary,
        border = if (selected) null else BorderStroke(GotestTheme.sizes.strokeThin, colors.borderSubtle),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = spacing.md + spacing.xxs, vertical = spacing.sm),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestFilterPillPreview() {
    GotestTheme {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            GotestFilterPill(text = "Semua", selected = true, onClick = {})
            GotestFilterPill(text = "Latihan", selected = false, onClick = {})
            GotestFilterPill(text = "Tryout", selected = false, onClick = {})
        }
    }
}
