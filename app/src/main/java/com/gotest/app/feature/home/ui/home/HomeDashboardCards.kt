package com.gotest.app.feature.home.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.gotest.app.BuildConfig
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestDivider
import com.gotest.app.core.designsystem.component.serverImage
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.home.domain.DashboardBanner
import com.gotest.app.feature.home.domain.MitraTaskCard
import com.gotest.app.feature.home.domain.SchoolSpace
import com.gotest.app.feature.home.domain.WeeklyLeaderboard
import com.gotest.app.feature.home.domain.WeeklyRow
import com.gotest.app.feature.home.ui.components.HomeDimens
import java.time.OffsetDateTime

/** Web DashboardBanner: the content team's image; a tap opens its link. */
@Composable
internal fun HomeBanner(
    banner: DashboardBanner,
    onOpen: ((href: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val href = banner.href?.takeIf { SAFE_HREF.containsMatchIn(it) }
    AsyncImage(
        model = serverImage(banner.imageUrl, BuildConfig.API_BASE_URL),
        contentDescription = banner.altText.ifBlank { banner.title },
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .then(
                    if (href != null && onOpen != null) {
                        Modifier.clickable(role = Role.Button) { onOpen(href) }
                    } else {
                        Modifier
                    },
                ),
        contentScale = ContentScale.FillWidth,
    )
}

/** Web MitraTasksCard: how many tasks are to do today and how many are late, one tap to the Mitra space. */
@Composable
internal fun HomeMitraTaskCard(
    card: MitraTaskCard,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts =
        listOfNotNull(
            card.todayCount.takeIf { it > 0 }?.let { stringResource(R.string.home_tasks_today, it) },
            card.lateCount.takeIf { it > 0 }?.let { stringResource(R.string.home_tasks_late, it) },
        )
    HomeSpaceCard(
        label = stringResource(R.string.home_tasks_label),
        name = card.organizationName,
        line = parts.joinToString(" · "),
        action = stringResource(R.string.home_tasks_action),
        onAction = onOpen,
        modifier = modifier,
    )
}

/**
 * Web SchoolSpaceCard (DEC-088): the next Try Out, one tap away. A student opens the Mitra space; staff open the
 * portal, which is on gotest.id only (A2b-2).
 */
@Composable
internal fun HomeSchoolSpaceCard(
    space: SchoolSpace,
    onOpenRuang: () -> Unit,
    onOpenPortal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = space.nextSession
    val line =
        when {
            next == null -> stringResource(R.string.home_space_no_session)
            next.isRunning -> stringResource(R.string.home_space_running, next.title)
            else -> stringResource(R.string.home_space_next, next.title, windowInstant(next.startsAt))
        }
    HomeSpaceCard(
        label = stringResource(if (space.isStudent) R.string.home_space_ruang else R.string.home_space_portal),
        name = space.name,
        line = line,
        action =
            stringResource(
                when {
                    !space.isStudent -> R.string.home_space_open_portal
                    next?.isRunning == true -> R.string.home_space_work_now
                    else -> R.string.home_space_open_ruang
                },
            ),
        onAction = if (space.isStudent) onOpenRuang else onOpenPortal,
        modifier = modifier,
    )
}

@Composable
private fun HomeSpaceCard(
    label: String,
    name: String,
    line: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textBrandDark)
            Text(text = name, style = MaterialTheme.typography.titleSmall, color = GotestTheme.colors.textPrimary)
            Text(text = line, style = MaterialTheme.typography.bodySmall, color = GotestTheme.colors.textSecondary)
        }
        GotestButton(text = action, onClick = onAction, modifier = Modifier.fillMaxWidth())
    }
}

/** Web MiniLeaderboard: the week's top three, and the viewer below them when not among them. */
@Composable
internal fun HomeWeeklyLeaderboard(
    leaderboard: WeeklyLeaderboard,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val current = leaderboard.current?.takeIf { row -> leaderboard.top.none { it.userId == row.userId } }
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(R.string.home_leaderboard_title),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.home_leaderboard_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        if (leaderboard.top.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.home_leaderboard_empty_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = GotestTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_leaderboard_empty_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column {
                LeaderboardHead()
                leaderboard.top.forEach { row ->
                    LeaderboardLine(row)
                    GotestDivider()
                }
            }
        }
        current?.let { row ->
            Text(
                text = "···",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            LeaderboardLine(
                row,
                Modifier.clip(MaterialTheme.shapes.small).background(GotestTheme.colors.bgSubtle),
            )
        }
    }
}

@Composable
private fun LeaderboardHead() {
    val style = MaterialTheme.typography.labelSmall
    val color = GotestTheme.colors.textSecondary
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = GotestTheme.spacing.xs)) {
        Text("#", Modifier.width(RANK_WIDTH), style = style, color = color, textAlign = TextAlign.Center)
        Text(stringResource(R.string.home_leaderboard_name), Modifier.weight(1f), style = style, color = color)
        Text(
            stringResource(R.string.home_leaderboard_points),
            Modifier.width(POINTS_WIDTH),
            style = style,
            color = color,
        )
    }
    GotestDivider()
}

@Composable
private fun LeaderboardLine(
    row: WeeklyRow,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = GotestFormat.number(row.rank),
            modifier = Modifier.width(RANK_WIDTH),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(INITIALS_SIZE)
                        .background(GotestTheme.colors.bgBrandSoft, GotestTheme.shapes.full),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials(row.name),
                    style = MaterialTheme.typography.labelSmall,
                    color = GotestTheme.colors.textBrandDark,
                )
            }
            Text(
                text = row.name,
                style = MaterialTheme.typography.labelMedium,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.home_leaderboard_points_value, row.points),
            modifier = Modifier.width(POINTS_WIDTH),
            style = MaterialTheme.typography.labelMedium,
            color = GotestTheme.colors.textPrimary,
        )
    }
}

/** Web `initials`: the first letter of the first two words. */
private fun initials(name: String): String =
    name
        .split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

/** Web `formatWindowInstant`: "12 Des 2026, 08.00 WIB". */
private fun windowInstant(value: String): String =
    runCatching { GotestFormat.windowInstant(OffsetDateTime.parse(value).toInstant()) }.getOrDefault("")

/** Same rule as the web: an internal path or an https link only. */
private val SAFE_HREF = Regex("""^(/(?![/\\])|https://)""", RegexOption.IGNORE_CASE)

private val RANK_WIDTH = HomeDimens.leaderboardRankWidth
private val POINTS_WIDTH = HomeDimens.leaderboardPointsWidth
private val INITIALS_SIZE = HomeDimens.leaderboardInitials

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeDashboardCardsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            HomePreviewData.page.mitraTasks.forEach { HomeMitraTaskCard(card = it, onOpen = {}) }
            HomePreviewData.page.schoolSpaces.forEach {
                HomeSchoolSpaceCard(
                    space = it,
                    onOpenRuang = {},
                    onOpenPortal = {},
                )
            }
            HomeWeeklyLeaderboard(leaderboard = HomePreviewData.page.leaderboard)
        }
    }
}
