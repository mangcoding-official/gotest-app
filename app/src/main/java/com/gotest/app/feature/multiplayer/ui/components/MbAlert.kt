package com.gotest.app.feature.multiplayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme

/** "Type" of the design's "MB/Alert". */
enum class MbAlertType { Error, Warning, Info, Success }

private data class AlertStyle(
    val container: Color,
    val content: Color,
    @param:DrawableRes val icon: Int,
)

private fun GotestColors.alertStyle(type: MbAlertType): AlertStyle =
    when (type) {
        MbAlertType.Error -> AlertStyle(statusDangerSoft, statusDanger, GotestIcons.Bold.CloseCircle)
        MbAlertType.Warning -> AlertStyle(statusWarningSoft, illustrationYellowDark, GotestIcons.Bold.InfoCircle)
        MbAlertType.Info -> AlertStyle(bgBlueSoft, illustrationBlueDark, GotestIcons.Bold.InfoCircle)
        MbAlertType.Success -> AlertStyle(bgGreenSoft, statusSuccess, GotestIcons.Bold.TickCircle)
    }

/** Inline message with an icon, a title and a short explanation ("MB/Alert"). */
@Composable
fun MbAlert(
    type: MbAlertType,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    val style = GotestTheme.colors.alertStyle(type)
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(style.container, MaterialTheme.shapes.small)
                .padding(horizontal = spacing.lg, vertical = spacing.md)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(style.icon),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = style.content,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = style.content)
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = style.content)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MbAlertPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            MbAlertType.entries.forEach { type ->
                MbAlert(
                    type = type,
                    title = "Judul pesan",
                    message = "Penjelasan singkat apa yang terjadi dan apa yang bisa dilakukan.",
                )
            }
        }
    }
}
