package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * White container with the design's hairline border. Defaults to radius/lg and 16dp padding;
 * hero cards pass [GotestTheme.shapes.largeIncreased].
 */
@Composable
fun GotestCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = GotestTheme.colors.bgApp,
    showBorder: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(GotestTheme.spacing.lg),
    verticalSpacing: Dp = GotestTheme.spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border =
        if (showBorder) BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle) else null
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = border,
            content = body,
        )
    } else {
        Surface(modifier = modifier, shape = shape, color = containerColor, border = border, content = body)
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestCardPreview() {
    GotestTheme {
        GotestCard(modifier = Modifier.fillMaxWidth().padding(GotestTheme.spacing.lg)) {
            Text(text = "Materi yang diujikan", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Latihan persiapan UTBK-SNBT dengan 20 soal.",
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}
