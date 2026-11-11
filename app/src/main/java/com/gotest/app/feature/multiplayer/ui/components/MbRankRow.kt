package com.gotest.app.feature.multiplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** "Kind" of the design's "MB/Rank Row". */
enum class RankRowKind { Champion, Default, You }

/** Fixed columns so the ranking header and rows line up. */
private object RankColumns {
    val position = 32.dp
    val correct = 72.dp
    val time = 48.dp
}

/** One line of the final ranking ("MB/Rank Row"). */
@Composable
fun MbRankRow(
    position: Int,
    name: String,
    avatarUrl: Any?,
    correctLabel: String,
    timeLabel: String,
    kind: RankRowKind,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val (container, content) =
        when (kind) {
            RankRowKind.Champion -> colors.illustrationYellowSoft to colors.illustrationYellowDark
            RankRowKind.Default -> colors.bgApp to colors.textPrimary
            RankRowKind.You -> colors.bgSubtle to colors.textPrimary
        }
    val timeColor = if (kind == RankRowKind.Champion) content else colors.textSecondary
    RankLine(
        modifier =
            modifier
                .background(container, MaterialTheme.shapes.small)
                .semantics(mergeDescendants = true) {},
        verticalPadding = GotestTheme.spacing.md,
        position = {
            if (kind == RankRowKind.Champion) {
                Icon(
                    painter = painterResource(GotestIcons.Bold.Award),
                    contentDescription = stringResource(R.string.multiplayer_cd_rank, position),
                    modifier = Modifier.size(GotestTheme.sizes.iconMd),
                    tint = colors.statusWarning,
                )
            } else {
                Text(text = position.toString(), style = MaterialTheme.typography.labelLarge, color = content)
            }
        },
        avatar = { GotestAvatar(model = avatarUrl, contentDescription = null, size = GotestTheme.sizes.avatarSm) },
        name = { RankText(name, MaterialTheme.typography.labelLarge, content, Modifier.fillMaxWidth()) },
        correct = { RankText(correctLabel, MaterialTheme.typography.labelLarge, content) },
        time = { RankText(timeLabel, MaterialTheme.typography.bodyMedium, timeColor, align = TextAlign.End) },
    )
}

/** Column titles above the [MbRankRow]s. */
@Composable
fun MbRankHeader(modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelMedium
    val color = GotestTheme.colors.textSecondary
    RankLine(
        modifier = modifier,
        verticalPadding = GotestTheme.spacing.sm,
        position = { RankText(stringResource(R.string.multiplayer_rank_column_position), style, color) },
        avatar = { Spacer(Modifier.size(GotestTheme.sizes.avatarSm)) },
        name = { RankText(stringResource(R.string.multiplayer_rank_column_name), style, color) },
        correct = { RankText(stringResource(R.string.multiplayer_rank_column_correct), style, color) },
        time = { RankText(stringResource(R.string.multiplayer_rank_column_time), style, color, align = TextAlign.End) },
    )
}

@Composable
private fun RankLine(
    verticalPadding: Dp,
    position: @Composable () -> Unit,
    avatar: @Composable () -> Unit,
    name: @Composable () -> Unit,
    correct: @Composable () -> Unit,
    time: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = GotestTheme.spacing.lg, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(RankColumns.position), contentAlignment = Alignment.CenterStart) { position() }
        avatar()
        Box(modifier = Modifier.weight(1f)) { name() }
        Box(modifier = Modifier.width(RankColumns.correct)) { correct() }
        Box(modifier = Modifier.width(RankColumns.time), contentAlignment = Alignment.CenterEnd) { time() }
    }
}

@Composable
private fun RankText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(showBackground = true)
@Composable
private fun MbRankRowPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            MbRankHeader()
            MbRankRow(1, "Ardy P.", null, "18 benar", "12:41", RankRowKind.Champion)
            MbRankRow(2, "Nadia P.", null, "16 benar", "13:20", RankRowKind.Default)
            MbRankRow(3, "Gilang P. (Kamu)", null, "14 benar", "14:05", RankRowKind.You)
        }
    }
}
