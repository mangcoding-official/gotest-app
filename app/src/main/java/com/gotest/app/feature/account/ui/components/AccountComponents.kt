package com.gotest.app.feature.account.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** Sizes the account frames use that have no design token. */
internal object AccountDimens {
    /** Trailing arrow, chevron and small tile glyphs. */
    val iconSmall = 18.dp

    /** "icon-box" of the design's "Mobile/Menu Row". */
    val menuIconBox = 32.dp

    /** Rounded tiles in sheet rows, contact tiles and payment rows. */
    val compactTile = 36.dp
    val compactTileRadius = 10.dp

    /** Camera glyph and white ring of the badge on the profile avatar. */
    val avatarBadgeIcon = 12.dp
    val avatarBadgeRing = 2.dp

    /** "Foto Profil" sheet preview. */
    val photoPreviewHeight = 230.dp
    val photoPreviewAvatar = 72.dp
    val skeletonLong = 95.dp
    val skeletonShort = 61.dp
    val skeletonThick = 12.dp
    val skeletonThin = 8.dp

    /** Radio and checkbox of the Pengaturan sub-screens. */
    val selectionControl = 20.dp
    val selectionControlStroke = 2.dp
    val radioDot = 10.dp
    val checkboxRadius = 6.dp
    val checkGlyph = 14.dp

    /** Label column of the Data Pribadi rows. */
    val personalDataLabelWidth = 120.dp
}

/** Box size, glyph size and corner radius of [AccountIconTile]. */
enum class AccountTileSize {
    /** 32dp box, 20dp glyph: menu rows. */
    Menu,

    /** 36dp box, 18dp glyph: sheet rows, contact tiles, payment rows. */
    Compact,

    /** 40dp box, 20dp glyph: help categories. */
    Medium,

    /** 40dp box, 20dp glyph, 10dp corners: Jenjang Utama options and the Unduh Data Akun intro. */
    Choice,

    /** 56dp box, 28dp glyph: the badge heading Ganti Password and Hapus Akun. */
    Large,
}

@Immutable
private data class TileMetrics(
    val box: Dp,
    val icon: Dp,
    val shape: Shape,
)

@Composable
private fun tileMetrics(size: AccountTileSize): TileMetrics {
    val sizes = GotestTheme.sizes
    val shapes = MaterialTheme.shapes
    return when (size) {
        AccountTileSize.Menu -> {
            TileMetrics(AccountDimens.menuIconBox, sizes.iconMd, shapes.small)
        }

        AccountTileSize.Compact -> {
            TileMetrics(
                AccountDimens.compactTile,
                AccountDimens.iconSmall,
                RoundedCornerShape(AccountDimens.compactTileRadius),
            )
        }

        AccountTileSize.Medium -> {
            TileMetrics(sizes.iconButton, sizes.iconMd, shapes.medium)
        }

        AccountTileSize.Choice -> {
            TileMetrics(sizes.iconButton, sizes.iconMd, RoundedCornerShape(AccountDimens.compactTileRadius))
        }

        AccountTileSize.Large -> {
            TileMetrics(sizes.dialogIcon, sizes.iconXl, shapes.large)
        }
    }
}

/** Rounded square holding a single glyph, e.g. the "icon-box" of a menu row. */
@Composable
fun AccountIconTile(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    size: AccountTileSize = AccountTileSize.Menu,
) {
    val metrics = tileMetrics(size)
    Box(
        modifier =
            modifier
                .size(metrics.box)
                .background(containerColor, metrics.shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(metrics.icon),
            tint = tint,
        )
    }
}

/** Muted arrow at the end of tappable rows. */
@Composable
fun AccountRowArrow(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(GotestIcons.Linear.ArrowRight),
        contentDescription = null,
        modifier = modifier.size(AccountDimens.iconSmall),
        tint = GotestTheme.colors.iconMuted,
    )
}

/** White rounded list container ("list" frames): rows sit edge to edge without dividers. */
@Composable
fun AccountListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(GotestTheme.spacing.none),
        verticalSpacing = GotestTheme.spacing.none,
        content = content,
    )
}

/**
 * Section heading above a list. Profil uses the [muted] grey label; the other account screens
 * use the primary colour with an optional [subtitle].
 */
@Composable
fun AccountSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    muted: Boolean = false,
) {
    val colors = GotestTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xxs)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = if (muted) colors.textSecondary else colors.textPrimary,
        )
        if (subtitle != null) {
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

/** Colour role of [AccountMenuRow]. */
enum class AccountMenuTone { Default, Danger }

/** The design's "Mobile/Menu Row": icon box, label and a muted arrow, 48dp tall. */
@Composable
fun AccountMenuRow(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: AccountMenuTone = AccountMenuTone.Default,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val danger = tone == AccountMenuTone.Danger
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = colors.bgApp) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountIconTile(icon = icon, tint = if (danger) colors.statusDanger else colors.iconDefault)
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (danger) colors.statusDanger else colors.textPrimary,
            )
            AccountRowArrow()
        }
    }
}

/** Small tinted info box, e.g. the blue email note on Edit Informasi Akun. */
@Composable
fun AccountNote(
    text: String,
    @DrawableRes icon: Int,
    containerColor: Color,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(GotestTheme.sizes.iconSm),
                tint = iconColor,
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
    }
}

/** White bordered "hint" card: an 18dp glyph and a muted note (Jenjang Utama, Unduh Data Akun). */
@Composable
fun AccountHint(
    text: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = GotestTheme.colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(AccountDimens.iconSmall),
                tint = GotestTheme.colors.iconDefault,
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

/** 20dp radio: a dark ring with a dot when [selected], a grey ring otherwise. Purely visual; the row is selectable. */
@Composable
fun AccountRadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = GotestTheme.shapes.full
    Box(
        modifier =
            modifier
                .size(AccountDimens.selectionControl)
                .border(
                    width = AccountDimens.selectionControlStroke,
                    color = if (selected) colors.borderBrandDark else colors.borderStrong,
                    shape = shape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(AccountDimens.radioDot).background(colors.bgBrandDark, shape))
        }
    }
}

/** 20dp checkbox: dark fill with a white tick when [checked], a grey outline otherwise. Purely visual. */
@Composable
fun AccountCheckboxIndicator(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val shape = RoundedCornerShape(AccountDimens.checkboxRadius)
    val box = modifier.size(AccountDimens.selectionControl)
    if (checked) {
        Box(modifier = box.background(colors.bgBrandDark, shape), contentAlignment = Alignment.Center) {
            CheckGlyph(color = colors.bgApp, modifier = Modifier.size(AccountDimens.checkGlyph))
        }
    } else {
        Box(modifier = box.border(AccountDimens.selectionControlStroke, colors.borderStrong, shape))
    }
}

/** The design's lucide "check" (24-unit grid: 20,6 → 9,17 → 4,12), stroked 2dp at 14dp. */
@Composable
private fun CheckGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val strokeWidth = AccountDimens.selectionControlStroke
    Canvas(modifier = modifier) {
        val unit = size.minDimension / CHECK_GRID
        val path =
            Path().apply {
                moveTo(CHECK_END_X * unit, CHECK_END_Y * unit)
                lineTo(CHECK_CORNER_X * unit, CHECK_CORNER_Y * unit)
                lineTo(CHECK_START_X * unit, CHECK_START_Y * unit)
            }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private const val CHECK_GRID = 24f
private const val CHECK_END_X = 20f
private const val CHECK_END_Y = 6f
private const val CHECK_CORNER_X = 9f
private const val CHECK_CORNER_Y = 17f
private const val CHECK_START_X = 4f
private const val CHECK_START_Y = 12f

/** Bordered white card with the radius/lg corners, used for single blocks like contact tiles. */
@Composable
fun AccountOutlinedSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = GotestTheme.colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountMenuRowPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            AccountSectionTitle(title = "Akun", muted = true)
            AccountListCard {
                AccountMenuRow(label = "Edit Informasi Akun", icon = GotestIcons.Linear.UserEdit, onClick = {})
                AccountMenuRow(label = "Ganti Password", icon = GotestIcons.Linear.Lock, onClick = {})
                AccountMenuRow(
                    label = "Keluar",
                    icon = GotestIcons.Linear.Logout,
                    onClick = {},
                    tone = AccountMenuTone.Danger,
                )
            }
            AccountNote(
                text = "Jika email diubah, kami akan mengirim link verifikasi ke email baru.",
                icon = GotestIcons.Linear.InfoCircle,
                containerColor = GotestTheme.colors.bgBlueSoft,
                iconColor = GotestTheme.colors.bgBlue,
                textColor = GotestTheme.colors.bgBlueDark,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountSelectionControlsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md)) {
                AccountRadioIndicator(selected = true)
                AccountRadioIndicator(selected = false)
                AccountCheckboxIndicator(checked = true)
                AccountCheckboxIndicator(checked = false)
            }
            AccountHint(
                text = "Riwayat dan skor kamu tetap tersimpan saat jenjang diubah.",
                icon = GotestIcons.Linear.InfoCircle,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountIconTilePreview() {
    GotestTheme {
        Row(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountTileSize.entries.forEach { size ->
                AccountIconTile(
                    icon = GotestIcons.Bold.Lock,
                    tint = GotestTheme.colors.iconBrand,
                    containerColor = GotestTheme.colors.bgBrandSoft,
                    size = size,
                )
            }
        }
    }
}
