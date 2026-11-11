package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme

/** "Tone" property of the design's "Mobile/Chip". */
enum class GotestChipTone { Blue, Green, Violet, Yellow, Red, Grey, White }

private data class ChipColors(
    val container: Color,
    val content: Color,
)

private fun GotestColors.chipColors(tone: GotestChipTone): ChipColors =
    when (tone) {
        GotestChipTone.Blue -> ChipColors(illustrationBlueSoft, illustrationBlueDark)
        GotestChipTone.Green -> ChipColors(illustrationGreenSoft, illustrationGreenDark)
        GotestChipTone.Violet -> ChipColors(illustrationBrandSoft, illustrationBrandDark)
        GotestChipTone.Yellow -> ChipColors(illustrationYellowSoft, illustrationYellowDark)
        GotestChipTone.Red -> ChipColors(illustrationRedSoft, illustrationRedDark)
        GotestChipTone.Grey -> ChipColors(illustrationGreySoft, illustrationGreyDark)
        GotestChipTone.White -> ChipColors(bgApp, illustrationGreyDark)
    }

/** Non-interactive status/category label ("Mobile/Chip"). */
@Composable
fun GotestChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: GotestChipTone = GotestChipTone.Blue,
    @DrawableRes leadingIcon: Int? = null,
) {
    val colors = GotestTheme.colors.chipColors(tone)
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier,
        shape = GotestTheme.shapes.full,
        color = colors.container,
        contentColor = colors.content,
        border =
            if (tone == GotestChipTone.White) {
                BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle)
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = spacing.md, vertical = spacing.xxs)),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconXs),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestChipPreview() {
    GotestTheme {
        FlowRow(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            GotestChipTone.entries.forEach { tone -> GotestChip(text = "UTBK-SNBT", tone = tone) }
            GotestChip(text = "Premium", tone = GotestChipTone.Yellow, leadingIcon = GotestIcons.Bold.Award)
        }
    }
}
