package com.gotest.app.feature.assistant.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** The AI chat background: white fading into bg/brand-soft. */
@Composable
internal fun assistantBackgroundBrush(): Brush =
    Brush.verticalGradient(listOf(GotestTheme.colors.bgApp, GotestTheme.colors.bgBrandSoft))

/** "Mobile/AI Avatar": dark brand circle with the AI chat glyph. */
@Composable
internal fun AssistantAvatar(
    modifier: Modifier = Modifier,
    size: Dp = GotestTheme.sizes.avatarSm,
    iconSize: Dp = GotestTheme.sizes.iconSm,
) {
    val colors = GotestTheme.colors
    Box(
        modifier =
            modifier
                .size(size)
                .background(colors.bgBrandDark, GotestTheme.shapes.full)
                .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, GotestTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(GotestIcons.AiChat),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = colors.iconOnBrand,
        )
    }
}

/**
 * Header of every AI chat frame: back (or close) button, avatar with "Asisten Gotest" and the
 * green online status, and the menu button.
 */
@Composable
internal fun AssistantTopBar(
    @DrawableRes navigationIcon: Int,
    navigationContentDescription: String,
    onNavigationClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    start = spacing.screenHorizontal,
                    top = spacing.sm,
                    end = spacing.screenHorizontal,
                    bottom = spacing.md,
                ),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestIconButton(
            icon = navigationIcon,
            contentDescription = navigationContentDescription,
            onClick = onNavigationClick,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistantAvatar()
            Column {
                Text(
                    text = stringResource(R.string.assistant_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OnlineStatus()
            }
        }
        GotestIconButton(
            icon = GotestIcons.Linear.Menu,
            contentDescription = stringResource(R.string.assistant_menu),
            onClick = onMenuClick,
        )
    }
}

@Composable
private fun OnlineStatus(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(GotestTheme.sizes.badgeDot)
                    .background(GotestTheme.colors.bgGreen, GotestTheme.shapes.full),
        )
        Text(
            text = stringResource(R.string.assistant_status),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Mobile/Prompt Chip": question icon and a brand-dark label; tapping sends or opens it. */
@Composable
internal fun AssistantPromptChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = GotestTheme.colors.bgApp,
) {
    val spacing = GotestTheme.spacing
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = GotestTheme.shapes.full,
        color = containerColor,
        border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Linear.MessageQuestion),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconSm),
                tint = GotestTheme.colors.iconBrand,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = GotestTheme.colors.textBrandDark,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssistantComponentsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.background(assistantBackgroundBrush()),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            AssistantTopBar(
                navigationIcon = GotestIcons.Linear.ArrowLeft,
                navigationContentDescription = "",
                onNavigationClick = {},
                onMenuClick = {},
            )
            Row(
                modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            ) {
                AssistantPromptChip(text = "Mulai tryout", onClick = {})
                AssistantPromptChip(
                    text = "Lihat peringkatku",
                    onClick = {},
                    containerColor = GotestTheme.colors.bgSubtle,
                )
            }
        }
    }
}
