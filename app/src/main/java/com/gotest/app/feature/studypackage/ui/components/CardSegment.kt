package com.gotest.app.feature.studypackage.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Where a lazy item sits inside a white container card that spans several items. */
internal enum class CardSegmentPosition { Top, Middle, Bottom }

/**
 * The design wraps whole lists in one white bordered card. In a lazy list each item draws its
 * own slice of that card, so rows stay lazy while the container still looks continuous.
 */
@Composable
internal fun CardSegment(
    position: CardSegmentPosition,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .cardSegment(
                    position = position,
                    shape = MaterialTheme.shapes.large,
                    color = GotestTheme.colors.bgApp,
                    borderColor = GotestTheme.colors.borderSubtle,
                    borderWidth = GotestTheme.sizes.strokeThin,
                ),
        content = content,
    )
}

private fun Modifier.cardSegment(
    position: CardSegmentPosition,
    shape: Shape,
    color: Color,
    borderColor: Color,
    borderWidth: Dp,
): Modifier =
    drawBehind {
        // Draw the full card outline taller than this slice and clip it, so only the slice's
        // own corners and edges show.
        val overdraw = size.width
        val stroke = borderWidth.toPx()
        val (offsetY, height) =
            when (position) {
                CardSegmentPosition.Top -> 0f to size.height + overdraw
                CardSegmentPosition.Middle -> -overdraw to size.height + overdraw * 2
                CardSegmentPosition.Bottom -> -overdraw to size.height + overdraw
            }
        val outline = shape.createOutline(Size(size.width - stroke, height - stroke), layoutDirection, this)
        clipRect {
            translate(left = stroke / 2, top = offsetY + stroke / 2) {
                drawOutline(outline, color)
                drawOutline(outline, borderColor, style = Stroke(width = stroke))
            }
        }
    }

@Preview(showBackground = true)
@Composable
private fun CardSegmentPreview() {
    GotestTheme {
        Column(modifier = Modifier.padding(GotestTheme.spacing.lg)) {
            CardSegment(CardSegmentPosition.Top, Modifier.height(GotestTheme.spacing.lg))
            CardSegment(CardSegmentPosition.Middle) {
                Text(
                    text = "Latihan September Session 1",
                    modifier = Modifier.padding(horizontal = GotestTheme.spacing.lg),
                )
            }
            CardSegment(CardSegmentPosition.Bottom, Modifier.height(GotestTheme.spacing.lg))
        }
    }
}
