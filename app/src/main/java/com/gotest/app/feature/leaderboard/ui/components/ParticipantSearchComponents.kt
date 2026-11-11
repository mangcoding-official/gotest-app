package com.gotest.app.feature.leaderboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.ui.GotestFormat

/** Opacity of the brand fill behind the student's own initials, so it shows on the grey row. */
private const val OWN_AVATAR_ALPHA = 0.3f
private const val INITIALS_LENGTH = 2

/** White card of participant search results (frame 21556); the student's row is grey. */
@Composable
fun ParticipantResultList(
    entries: List<LeaderboardEntry>,
    modifier: Modifier = Modifier,
) {
    GotestCard(
        modifier = modifier,
        contentPadding = PaddingValues(GotestTheme.spacing.sm),
        verticalSpacing = GotestTheme.spacing.none,
    ) {
        entries.forEach { entry -> ParticipantResultRow(entry = entry) }
    }
}

/** Rank, initials, name with "Selesai dalam 2:51:20", and points. */
@Composable
fun ParticipantResultRow(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val spacing = GotestTheme.spacing
    val rankDescription = stringResource(R.string.leaderboard_cd_podium_rank, entry.rank)
    val rowColor = if (entry.isCurrentUser) colors.bgSubtle else Color.Transparent
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(rowColor, MaterialTheme.shapes.medium)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = GotestFormat.number(entry.rank),
            modifier =
                Modifier
                    .width(LeaderboardDimens.searchRankColumn)
                    .clearAndSetSemantics { contentDescription = rankDescription },
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            maxLines = 1,
            softWrap = false,
        )
        InitialsAvatar(name = entry.name, isCurrentUser = entry.isCurrentUser)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = displayName(entry),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entryCaption(entry, LeaderboardMetric.FinishTime),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.leaderboard_points, GotestFormat.number(entry.points)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Immutable
private data class AvatarTone(
    val container: Color,
    val content: Color,
)

/** Soft circle with up to two initials; the tone follows the name so it stays put between searches. */
@Composable
private fun InitialsAvatar(
    name: String,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    val tone =
        if (isCurrentUser) {
            AvatarTone(colors.bgBrand.copy(alpha = OWN_AVATAR_ALPHA), colors.bgBrand)
        } else {
            val tones =
                listOf(
                    AvatarTone(colors.bgBrandSoft, colors.bgBrand),
                    AvatarTone(colors.illustrationGreenSoft, colors.textPrimary),
                    AvatarTone(colors.illustrationBlueSoft, colors.bgBlueDark),
                    AvatarTone(colors.illustrationYellowSoft, colors.bgYellowDark),
                )
            tones[Math.floorMod(name.hashCode(), tones.size)]
        }
    Box(
        modifier =
            modifier
                .size(LeaderboardDimens.searchAvatar)
                .background(tone.container, GotestTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials(name), style = MaterialTheme.typography.labelLarge, color = tone.content)
    }
}

/** "Gilang P." → "GP", "Mia" → "M". */
private fun initials(name: String): String =
    name
        .split(' ')
        .mapNotNull { word -> word.firstOrNull(Char::isLetter) }
        .take(INITIALS_LENGTH)
        .joinToString("")
        .uppercase()

@Preview(showBackground = true)
@Composable
private fun ParticipantResultListPreview() {
    GotestTheme {
        ParticipantResultList(
            entries =
                listOf(
                    LeaderboardEntry(12, "Gilang P.", null, 104, null, 10_280, 1, true),
                    LeaderboardEntry(58, "Gilbert Andreas", null, 88, null, 10_683, 0, false),
                    LeaderboardEntry(143, "Gita Saraswati", null, 61, null, 11_147, 0, false),
                ),
            modifier = Modifier.padding(GotestTheme.spacing.lg),
        )
    }
}
