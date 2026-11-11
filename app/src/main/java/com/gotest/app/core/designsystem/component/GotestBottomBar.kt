package com.gotest.app.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

private const val INACTIVE_ITEM_ALPHA = 0.5f

/** One tab of [GotestBottomBar]. */
@Immutable
data class GotestBottomBarItem(
    val label: String,
    @param:DrawableRes val selectedIcon: Int,
    @param:DrawableRes val unselectedIcon: Int,
)

/**
 * The design's "Mobile/Navbar": a floating pill of tabs (only the active one shows its
 * label) next to the round AI assistant button.
 */
@Composable
fun GotestBottomBar(
    items: List<GotestBottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAssistantClick: () -> Unit,
    assistantContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val sizes = GotestTheme.sizes
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.bgSubtle))),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = GotestTheme.shapes.full,
                color = colors.bgApp,
                shadowElevation = sizes.elevationShadow,
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(spacing.xs)
                            .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        BottomBarTab(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            // The active tab hugs its label; the icon-only tabs share what is left, so the
                            // label still fits on phones narrower than the 440dp design frame.
                            modifier = if (index == selectedIndex) Modifier else Modifier.weight(1f),
                        )
                    }
                }
            }
            Surface(
                onClick = onAssistantClick,
                modifier =
                    Modifier
                        .size(sizes.navBarFab)
                        .semantics { contentDescription = assistantContentDescription },
                shape = GotestTheme.shapes.full,
                color = colors.bgBrandDark,
                contentColor = colors.iconOnBrand,
                border = BorderStroke(sizes.strokeThin, colors.borderSubtle),
                shadowElevation = sizes.elevationShadow,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(GotestIcons.AiChat),
                        contentDescription = null,
                        modifier = Modifier.size(sizes.iconMd),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarTab(
    item: GotestBottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val sizes = GotestTheme.sizes
    val shape = GotestTheme.shapes.full
    Row(
        modifier =
            modifier
                .height(sizes.navBarItem)
                .then(if (selected) Modifier.shadow(sizes.strokeThin, shape) else Modifier)
                .clip(shape)
                .background(if (selected) colors.bgSubtle else Color.Transparent)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .semantics { contentDescription = item.label }
                .padding(horizontal = if (selected) spacing.lg else spacing.none),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
            contentDescription = null,
            modifier = Modifier.size(sizes.iconMd),
            tint = if (selected) colors.iconBrandDark else colors.iconDefault.copy(alpha = INACTIVE_ITEM_ALPHA),
        )
        AnimatedVisibility(visible = selected) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textBrandDark,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GotestBottomBarPreview() {
    GotestTheme {
        GotestBottomBar(
            items =
                listOf(
                    GotestBottomBarItem("Dashboard", GotestIcons.Bold.Home2, GotestIcons.Linear.Home2),
                    GotestBottomBarItem("Paket", GotestIcons.Bold.TaskSquare, GotestIcons.Linear.TaskSquare),
                    GotestBottomBarItem("Riwayat", GotestIcons.Bold.Directbox, GotestIcons.Linear.Directbox),
                    GotestBottomBarItem("Peringkat", GotestIcons.Bold.Leaderboard, GotestIcons.Linear.Leaderboard),
                    GotestBottomBarItem("Profil", GotestIcons.Bold.User, GotestIcons.Linear.User),
                ),
            selectedIndex = 0,
            onSelect = {},
            onAssistantClick = {},
            assistantContentDescription = "Asisten Gotest",
        )
    }
}
