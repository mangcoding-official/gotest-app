package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/**
 * The design's "Mobile/Icon Button": a 40dp white square with a hairline border.
 * [showBadge] adds the red unread dot used on the notification bell.
 */
@Composable
fun GotestIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBadge: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = GotestTheme.colors
    val sizes = GotestTheme.sizes
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = colors.bgApp,
        contentColor = colors.iconDefault,
        border = BorderStroke(sizes.strokeThin, colors.borderSubtle),
    ) {
        Box(modifier = Modifier.size(sizes.iconButton), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(sizes.iconMd),
            )
            if (showBadge) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = -GotestTheme.spacing.sm, y = GotestTheme.spacing.sm)
                            .size(sizes.badgeDot)
                            .background(colors.statusDanger, GotestTheme.shapes.full),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestIconButtonPreview() {
    GotestTheme {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            GotestIconButton(icon = GotestIcons.Linear.ArrowLeft, contentDescription = null, onClick = {})
            GotestIconButton(icon = GotestIcons.Linear.SearchNormal, contentDescription = null, onClick = {})
            GotestIconButton(
                icon = GotestIcons.Linear.Notification,
                contentDescription = null,
                onClick = {},
                showBadge = true,
            )
        }
    }
}
