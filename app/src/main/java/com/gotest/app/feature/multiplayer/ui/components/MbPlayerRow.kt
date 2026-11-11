package com.gotest.app.feature.multiplayer.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.PlayerState

/** "State" of the design's "MB/Player Row". */
enum class PlayerRowState { Host, Joined, Playing, Finished, Empty }

private data class SeatBadge(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val container: Color,
    val content: Color,
)

private fun GotestColors.seatBadge(state: PlayerRowState): SeatBadge? =
    when (state) {
        PlayerRowState.Host -> {
            SeatBadge(
                R.string.multiplayer_seat_host,
                GotestIcons.Bold.Award,
                illustrationBrandSoft,
                illustrationBrandDark,
            )
        }

        PlayerRowState.Joined -> {
            SeatBadge(
                R.string.multiplayer_seat_ready,
                GotestIcons.Bold.TickCircle,
                illustrationGreenSoft,
                illustrationGreenDark,
            )
        }

        PlayerRowState.Finished -> {
            SeatBadge(
                R.string.multiplayer_seat_finished,
                GotestIcons.Bold.TickCircle,
                illustrationGreenSoft,
                illustrationGreenDark,
            )
        }

        // The design's clock glyph is not in GotestIcons; the calendar is the closest time icon.
        PlayerRowState.Playing -> {
            SeatBadge(
                R.string.multiplayer_seat_playing,
                GotestIcons.Linear.Calendar,
                illustrationYellowSoft,
                illustrationYellowDark,
            )
        }

        PlayerRowState.Empty -> {
            null
        }
    }

/** One seat of a lobby or of "Status pemain" ("MB/Player Row"). [PlayerRowState.Empty] is a free seat. */
@Composable
fun MbPlayerRow(
    name: String,
    subtitle: String,
    avatarUrl: Any?,
    state: PlayerRowState,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bgApp)
                .padding(horizontal = spacing.lg, vertical = spacing.md)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state == PlayerRowState.Empty) {
            EmptySeatAvatar()
        } else {
            GotestAvatar(model = avatarUrl, contentDescription = null, size = GotestTheme.sizes.avatarSm)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = if (state == PlayerRowState.Empty) colors.textPlaceholder else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        colors.seatBadge(state)?.let { SeatBadgeChip(it) }
    }
}

@Composable
private fun SeatBadgeChip(
    badge: SeatBadge,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .background(badge.container, GotestTheme.shapes.full)
                .padding(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(badge.icon),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconXs),
            tint = badge.content,
        )
        Text(
            text = stringResource(badge.label),
            modifier = Modifier.padding(horizontal = spacing.xs),
            style = GotestTheme.typography.labelCompact,
            color = badge.content,
        )
    }
}

@Composable
private fun EmptySeatAvatar(modifier: Modifier = Modifier) {
    val color = GotestTheme.colors.borderStrong
    val stroke = GotestTheme.sizes.strokeThin
    val dash = MultiplayerDimens.dash
    Box(
        modifier =
            modifier
                .size(GotestTheme.sizes.avatarSm)
                .drawBehind {
                    val width = stroke.toPx()
                    drawCircle(
                        color = color,
                        radius = (size.minDimension - width) / 2,
                        style =
                            Stroke(
                                width = width,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())),
                            ),
                    )
                },
    )
}

/** Seat badge for a player in the lobby or in "Status pemain". */
fun GamePlayer.rowState(): PlayerRowState =
    when (state) {
        PlayerState.Host -> PlayerRowState.Host
        PlayerState.Joined -> PlayerRowState.Joined
        PlayerState.Playing -> PlayerRowState.Playing
        PlayerState.Finished -> PlayerRowState.Finished
    }

/** Bordered list that separates seats with hairlines, as in the lobby and "Status pemain". */
@Composable
fun MbSeatList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(GotestTheme.colors.bgApp)
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, shape),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun MbPlayerRowPreview() {
    GotestTheme {
        MbSeatList(modifier = Modifier.padding(GotestTheme.spacing.lg)) {
            MbPlayerRow("Gilang Permana (Kamu)", "Pembuat permainan", null, PlayerRowState.Host)
            GotestDivider()
            MbPlayerRow("Ardy Pratama", "Bergabung 2 menit lalu", null, PlayerRowState.Joined)
            GotestDivider()
            MbPlayerRow("Nadia Putri", "Masih di dalam permainan", null, PlayerRowState.Playing)
            GotestDivider()
            MbPlayerRow("Rizky Ramadhan", "Selesai 1 menit lalu", null, PlayerRowState.Finished)
            GotestDivider()
            MbPlayerRow("Menunggu pemain...", "7 kursi tersisa", null, PlayerRowState.Empty)
        }
    }
}
