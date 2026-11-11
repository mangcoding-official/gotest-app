package com.gotest.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Title placement of [GotestTopAppBar]. */
enum class GotestTopAppBarStyle {
    /** Detail screens: back button, centred title, optional action ("app-bar" in the design). */
    Centered,

    /** Tab roots: left-aligned title with a subtitle ("header" in the design). */
    Leading,
}

/**
 * 56dp app bar used across the design. It pads itself for the status bar, so screens
 * can draw edge to edge.
 */
@Composable
fun GotestTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: GotestTopAppBarStyle = GotestTopAppBarStyle.Centered,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    val barModifier =
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = GotestTheme.sizes.appBarHeight)
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
    when (style) {
        GotestTopAppBarStyle.Centered -> {
            CenteredAppBarLayout(
                navigationIcon = navigationIcon,
                title = { AppBarTitle(title = title, subtitle = subtitle, centered = true) },
                actions = actions,
                modifier = barModifier,
            )
        }

        GotestTopAppBarStyle.Leading -> {
            Row(
                modifier = barModifier,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationIcon?.invoke()
                AppBarTitle(title = title, subtitle = subtitle, centered = false, modifier = Modifier.weight(1f))
                actions()
            }
        }
    }
}

/**
 * Centres the title on the whole bar, not on the space left between the slots, so a screen
 * with only a back button still matches the design. The title is kept clear of the wider slot
 * on both sides.
 */
@Composable
private fun CenteredAppBarLayout(
    navigationIcon: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = GotestTheme.spacing.md
    Layout(
        content = {
            Box(Modifier.layoutId(SLOT_NAVIGATION)) { navigationIcon?.invoke() }
            Box(Modifier.layoutId(SLOT_TITLE)) { title() }
            Row(
                modifier = Modifier.layoutId(SLOT_ACTIONS),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val navigation = measurables.first { it.layoutId == SLOT_NAVIGATION }.measure(loose)
        val actionRow = measurables.first { it.layoutId == SLOT_ACTIONS }.measure(loose)
        val side = maxOf(navigation.width, actionRow.width)
        val inset = if (side > 0) side + gap.roundToPx() else 0
        val width = constraints.maxWidth
        val titleText =
            measurables
                .first { it.layoutId == SLOT_TITLE }
                .measure(loose.copy(maxWidth = (width - 2 * inset).coerceAtLeast(0)))
        val height = maxOf(constraints.minHeight, navigation.height, titleText.height, actionRow.height)
        layout(width, height) {
            navigation.placeRelative(0, (height - navigation.height) / 2)
            titleText.placeRelative((width - titleText.width) / 2, (height - titleText.height) / 2)
            actionRow.placeRelative(width - actionRow.width, (height - actionRow.height) / 2)
        }
    }
}

@Composable
private fun AppBarTitle(
    title: String,
    subtitle: String?,
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = if (centered) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val SLOT_NAVIGATION = "navigation"
private const val SLOT_TITLE = "title"
private const val SLOT_ACTIONS = "actions"

/** The back arrow every detail screen puts in [GotestTopAppBar]'s navigation slot. */
@Composable
fun GotestBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GotestIconButton(
        icon = GotestIcons.Linear.ArrowLeft,
        contentDescription = stringResource(R.string.cd_back),
        onClick = onClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun GotestTopAppBarPreview() {
    GotestTheme {
        Column {
            GotestTopAppBar(
                title = "Detail Paket",
                navigationIcon = { GotestBackButton(onClick = {}) },
                actions = {
                    GotestIconButton(icon = GotestIcons.Linear.ArchiveAdd, contentDescription = null, onClick = {})
                },
            )
            GotestTopAppBar(title = "Ruang tunggu", navigationIcon = { GotestBackButton(onClick = {}) })
            GotestTopAppBar(
                title = "Paket Belajar",
                subtitle = "Pilih latihan atau tryout sesuai jenjangmu.",
                style = GotestTopAppBarStyle.Leading,
                actions = {
                    GotestIconButton(icon = GotestIcons.Linear.SearchNormal, contentDescription = null, onClick = {})
                },
            )
        }
    }
}
