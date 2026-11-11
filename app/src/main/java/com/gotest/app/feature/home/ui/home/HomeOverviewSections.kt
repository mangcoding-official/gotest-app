package com.gotest.app.feature.home.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonSize
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.component.GotestChip
import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.core.designsystem.component.GotestIconButton
import com.gotest.app.core.designsystem.component.GotestProgressBar
import com.gotest.app.core.designsystem.component.singleLineAutoSize
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.UserStats
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.home.ui.components.HomeDimens
import com.gotest.app.feature.home.ui.components.HomeIconTile
import com.gotest.app.feature.home.ui.components.HomeMetaDot
import kotlin.math.abs

private const val PERCENT = 100

/** Avatar, "Halo, Gilang" and the search and notification buttons. */
@Composable
internal fun HomeTopBar(
    profile: HomeProfileSummary,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GotestAvatar(model = profile.avatarUrl, contentDescription = null)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                text = stringResource(R.string.home_greeting, profile.firstName),
                style = MaterialTheme.typography.titleSmall,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_greeting_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            GotestIconButton(
                icon = GotestIcons.Linear.SearchNormal,
                contentDescription = stringResource(R.string.home_cd_search),
                onClick = onSearch,
            )
            GotestIconButton(
                icon = GotestIcons.Linear.Notification,
                contentDescription =
                    stringResource(
                        if (profile.hasUnreadNotifications) {
                            R.string.home_cd_notifications_unread
                        } else {
                            R.string.home_cd_notifications
                        },
                    ),
                onClick = onNotifications,
                showBadge = profile.hasUnreadNotifications,
            )
        }
    }
}

/** "Beranda / Promo Event" banner; the artwork carries its own "Lihat Promo" label. */
@Composable
internal fun HomePromoBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.banner_promo),
        contentDescription = stringResource(R.string.home_cd_promo),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onClick),
        contentScale = ContentScale.FillWidth,
    )
}

/** Three stat cards; [stats] null shows "-" (new student). */
@Composable
internal fun HomeStatsRow(
    stats: UserStats?,
    modifier: Modifier = Modifier,
) {
    val colors = GotestTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        val cardModifier = Modifier.weight(1f).fillMaxHeight()
        HomeStatCard(
            icon = GotestIcons.Linear.TaskSquare,
            containerColor = colors.statusSuccessSoft,
            iconColor = colors.illustrationGreenDark,
            value = stats?.let { GotestFormat.number(it.questionsAnswered) },
            trendPercent = stats?.questionsTrendPercent,
            label = R.string.home_stat_questions,
            modifier = cardModifier,
        )
        HomeStatCard(
            // The design's target icon is not exported; Award is the closest match.
            icon = GotestIcons.Linear.Award,
            containerColor = colors.bgBlueSoft,
            iconColor = colors.bgBlueDark,
            value = stats?.let { stringResource(R.string.percent_value, it.accuracyPercent) },
            trendPercent = stats?.accuracyTrendPercent,
            label = R.string.home_stat_accuracy,
            modifier = cardModifier,
        )
        HomeStatCard(
            icon = GotestIcons.Linear.TickCircle,
            containerColor = colors.bgBrandSoft,
            iconColor = colors.iconBrandDark,
            value = stats?.let { GotestFormat.number(it.packagesCompleted) },
            trendPercent = stats?.packagesTrendPercent,
            label = R.string.home_stat_packages,
            modifier = cardModifier,
        )
    }
}

@Composable
private fun HomeStatCard(
    @DrawableRes icon: Int,
    containerColor: Color,
    iconColor: Color,
    value: String?,
    trendPercent: Int?,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(
        modifier = modifier,
        contentPadding = PaddingValues(spacing.md),
        verticalSpacing = spacing.md,
    ) {
        HomeIconTile(
            icon = icon,
            containerColor = containerColor,
            iconColor = iconColor,
            shape = MaterialTheme.shapes.extraSmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value ?: stringResource(R.string.home_stat_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = GotestTheme.colors.textPrimary,
                )
                if (trendPercent != null) {
                    HomeTrendBadge(percent = trendPercent)
                }
            }
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
                // Three cards share a 360–412dp row on most phones; keeps "Akurasi Jawaban" on one line.
                autoSize = singleLineAutoSize(MaterialTheme.typography.bodySmall),
            )
        }
    }
}

/** Week-over-week change: red and down when negative, green and up otherwise. */
@Composable
private fun HomeTrendBadge(
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val rising = percent >= 0
    val color = if (rising) GotestTheme.colors.statusSuccess else GotestTheme.colors.statusDanger
    val description =
        stringResource(if (rising) R.string.home_cd_trend_up else R.string.home_cd_trend_down, abs(percent))
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.percent_value, abs(percent)),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
        // The design's boxed trend-line icon is not exported; arrows are the closest match.
        Icon(
            painter = painterResource(if (rising) GotestIcons.Linear.ArrowUp else GotestIcons.Linear.ArrowDown),
            contentDescription = null,
            modifier =
                Modifier
                    .size(GotestTheme.sizes.iconSm)
                    .border(GotestTheme.sizes.strokeThin, color, MaterialTheme.shapes.extraSmall),
            tint = color,
        )
    }
}

/** "Main Bareng · Baru" entry card. */
@Composable
internal fun HomeMultiplayerCard(
    onJoinGame: () -> Unit,
    onCreateGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(HomeDimens.multiplayerIconWidth)
                        .background(colors.bgBrandSoft, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(GotestIcons.Bold.People),
                    contentDescription = null,
                    modifier = Modifier.size(GotestTheme.sizes.iconXl),
                    tint = colors.iconBrandDark,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_multiplayer_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    GotestChip(text = stringResource(R.string.home_multiplayer_badge), tone = GotestChipTone.Violet)
                }
                Text(
                    text = stringResource(R.string.home_multiplayer_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.home_multiplayer_join),
                onClick = onJoinGame,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
            )
            GotestButton(
                text = stringResource(R.string.home_multiplayer_create),
                onClick = onCreateGame,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Lanjutkan" card for the package the student left unfinished. */
@Composable
internal fun HomeContinueCard(
    studyPackage: StudyPackage,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val progress = studyPackage.progress
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.lg) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = studyPackage.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HomePackageMeta(studyPackage = studyPackage)
            }
            GotestButton(
                text = stringResource(R.string.home_continue_action),
                onClick = onContinue,
                size = GotestButtonSize.Medium,
                trailingIcon = GotestIcons.Linear.ArrowRight,
            )
        }
        if (progress != null) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.bgSubtle, GotestTheme.shapes.full)
                            .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, GotestTheme.shapes.full)
                            .padding(spacing.xxs),
                ) {
                    GotestProgressBar(
                        progress = progress.fraction,
                        color = colors.bgBlue,
                        trackColor = Color.Transparent,
                        height = HomeDimens.continueProgressHeight,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_continue_progress, progress.correct, progress.total),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.percent_value, (progress.fraction * PERCENT).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

/** "UTBK-SNBT • 20 Soal • 45 Menit". */
@Composable
private fun HomePackageMeta(
    studyPackage: StudyPackage,
    modifier: Modifier = Modifier,
) {
    val parts =
        listOfNotNull(
            studyPackage.examLabel,
            stringResource(R.string.home_package_question_count, studyPackage.questionCount),
            studyPackage.durationMinutes?.let { stringResource(R.string.home_package_duration, it) },
        )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { index, part ->
            if (index > 0) HomeMetaDot()
            Text(
                text = part,
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
    }
}

/** Empty-state replacement for "Lanjutkan": points a new student at the catalogue. */
@Composable
internal fun HomeFirstPracticeCard(
    onBrowsePackages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(R.string.home_first_practice_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = GotestTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.home_first_practice_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = GotestTheme.colors.textSecondary,
                )
            }
            GotestButton(
                text = stringResource(R.string.home_first_practice_action),
                onClick = onBrowsePackages,
                size = GotestButtonSize.Medium,
                trailingIcon = GotestIcons.Linear.ArrowRight,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeTopBarPreview() {
    GotestTheme {
        HomeTopBar(
            profile = HomePreviewData.profile,
            onSearch = {},
            onNotifications = {},
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
        )
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeStatsRowPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            HomeStatsRow(stats = HomePreviewData.stats)
            HomeStatsRow(stats = null)
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun HomeCardsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
        ) {
            HomePromoBanner(onClick = {})
            HomeMultiplayerCard(onJoinGame = {}, onCreateGame = {})
            HomeContinueCard(studyPackage = HomePreviewData.inProgress, onContinue = {})
            HomeFirstPracticeCard(onBrowsePackages = {})
        }
    }
}
