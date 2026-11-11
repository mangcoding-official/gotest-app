package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Rounded determinate bar used for paket progress and topic accuracy. */
@Composable
fun GotestProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = GotestTheme.colors.bgBlue,
    trackColor: Color = GotestTheme.colors.bgGrey,
    height: Dp = GotestTheme.sizes.progressBarHeight,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val shape = GotestTheme.shapes.full
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(trackColor)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(shape)
                    .background(color),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestProgressBarPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            GotestProgressBar(progress = 0.25f)
            GotestProgressBar(progress = 0.75f, color = GotestTheme.colors.bgYellow)
            GotestProgressBar(progress = 0.8f, color = GotestTheme.colors.bgGreen)
        }
    }
}
