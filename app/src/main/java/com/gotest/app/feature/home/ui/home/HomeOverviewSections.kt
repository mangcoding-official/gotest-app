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
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.home.domain.ContinueAttempt
import com.gotest.app.feature.home.domain.DashboardStats
import com.gotest.app.feature.home.ui.components.HomeDimens
import com.gotest.app.feature.home.ui.components.HomeIconTile
import com.gotest.app.feature.home.ui.components.HomeMetaDot
import kotlin.math.roundToInt

private const val PERCENT = 100

/** Avatar, "Halo, Gilang" and the search and notification buttons. */
@Composable
internal fun HomeTopBar(
    profile: HomeProfileSummary,
    onSearch: () -> Unit,
    onNotifications: (() -> Unit)?,
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
            if (onNotifications != null) {
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
}

/**
 * Promo behind the bundled banner artwork ("Beranda / Promo Semangat Merdeka"). Once banners come
 * from the API, each one carries its own promo id.
 */
internal const val HOME_PROMO_ID = "promo_semangat_merdeka"

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
    stats: DashboardStats?,
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
            value = stats?.let { GotestFormat.number(it.questionsDone) },
            label = R.string.home_stat_questions,
            modifier = cardModifier,
        )
        HomeStatCard(
            // The design's target icon is not exported; Award is the closest match.
            icon = GotestIcons.Linear.Award,
            containerColor = colors.bgBlueSoft,
            iconColor = colors.bgBlueDark,
            value = stats?.let { stringResource(R.string.percent_value, it.accuracy) },
            label = R.string.home_stat_accuracy,
            modifier = cardModifier,
        )
        HomeStatCard(
            icon = GotestIcons.Linear.TickCircle,
            containerColor = colors.bgBrandSoft,
            iconColor = colors.iconBrandDark,
            value = stats?.let { GotestFormat.number(it.packagesDone) },
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
            Text(
                text = value ?: stringResource(R.string.home_stat_empty),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
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

/** Web ContinueCard: the attempt left unfinished, its progress, and "Lanjutkan Latihan" or "Lanjutkan Try Out". */
@Composable
internal fun HomeContinueCard(
    attempt: ContinueAttempt,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val label = stringResource(if (attempt.isTryout) R.string.home_continue_tryout else R.string.home_continue_practice)
    val total = attempt.questionCount
    val fraction = if (total > 0) attempt.answeredCount.toFloat() / total else 0f
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.lg) {
        GotestChip(text = label, tone = GotestChipTone.Violet)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = attempt.packageName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            HomeMeta(
                parts =
                    listOfNotNull(
                        attempt.categoryName ?: stringResource(R.string.home_category_general),
                        stringResource(R.string.home_package_question_count, attempt.questionCount),
                        attempt.durationMinutes?.let { stringResource(R.string.home_package_duration, it) },
                    ),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_continue_progress, attempt.answeredCount, total),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Text(
                    text = stringResource(R.string.percent_value, (fraction * PERCENT).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.bgSubtle, GotestTheme.shapes.full)
                        .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, GotestTheme.shapes.full)
                        .padding(spacing.xxs),
            ) {
                GotestProgressBar(
                    progress = fraction,
                    color = colors.bgBlue,
                    trackColor = Color.Transparent,
                    height = HomeDimens.continueProgressHeight,
                )
            }
        }
        GotestButton(
            text = label,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = GotestIcons.Linear.ArrowRight,
        )
    }
}

/** "UTBK-SNBT • 20 Soal • 45 Menit". */
@Composable
internal fun HomeMeta(
    parts: List<String>,
    modifier: Modifier = Modifier,
) {
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

/**
 * Web ContinueCard without an attempt in progress: a new student starts their first Latihan; one who finished
 * packages is told everything is done (review R-01).
 */
@Composable
internal fun HomeFirstPracticeCard(
    hasHistory: Boolean,
    onBrowsePackages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(modifier = modifier.fillMaxWidth(), verticalSpacing = spacing.md) {
        GotestChip(
            text =
                stringResource(
                    if (hasHistory) R.string.home_no_attempt_badge else R.string.home_first_practice_badge,
                ),
            tone = GotestChipTone.Violet,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text =
                    stringResource(
                        if (hasHistory) R.string.home_no_attempt_title else R.string.home_first_practice_title,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = GotestTheme.colors.textPrimary,
            )
            Text(
                text =
                    stringResource(
                        if (hasHistory) R.string.home_no_attempt_body else R.string.home_first_practice_body,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = GotestTheme.colors.textSecondary,
            )
        }
        GotestButton(
            text = stringResource(R.string.home_first_practice_action),
            onClick = onBrowsePackages,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = GotestIcons.Linear.ArrowRight,
        )
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
            HomeStatsRow(stats = HomePreviewData.page.stats)
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
            HomePreviewData.page.continueAttempt?.let { HomeContinueCard(attempt = it, onContinue = {}) }
            HomeFirstPracticeCard(hasHistory = false, onBrowsePackages = {})
        }
    }
}
