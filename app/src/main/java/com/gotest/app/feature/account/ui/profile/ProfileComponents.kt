package com.gotest.app.feature.account.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.account.ui.components.AccountDimens
import com.gotest.app.feature.account.ui.components.AccountPreviewData

/** "profile-card": tappable avatar, name, email, plan chip and the three stat tiles. */
@Composable
fun ProfileCard(
    profile: UserProfile,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    GotestCard(
        modifier = modifier.fillMaxWidth(),
        shape = GotestTheme.shapes.largeIncreased,
        verticalSpacing = spacing.lg,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatarButton(photo = profile.avatarUrl, onClick = onAvatarClick)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlanChip(isPremium = profile.subscription.isPremium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            StatTile(
                value = GotestFormat.number(profile.stats.questionsAnswered),
                label = stringResource(R.string.account_profile_stat_questions),
            )
            StatTile(
                value = stringResource(R.string.percent_value, profile.stats.accuracyPercent),
                label = stringResource(R.string.account_profile_stat_accuracy),
            )
            StatTile(
                value = GotestFormat.number(profile.stats.packagesCompleted),
                label = stringResource(R.string.account_profile_stat_packages),
            )
        }
    }
}

/** 64dp avatar with the camera badge from the "Foto Profil" frame; opens the photo sheet. */
@Composable
private fun ProfileAvatarButton(
    photo: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizes = GotestTheme.sizes
    val colors = GotestTheme.colors
    val description = stringResource(R.string.account_profile_avatar_cd)
    Box(modifier = modifier.size(sizes.avatarLg)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = description },
            shape = GotestTheme.shapes.full,
        ) {
            GotestAvatar(model = photo, contentDescription = null, size = sizes.avatarLg)
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(sizes.avatarXs)
                    .background(colors.bgBrandDark, GotestTheme.shapes.full)
                    .border(AccountDimens.avatarBadgeRing, colors.bgApp, GotestTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Bold.Camera),
                contentDescription = null,
                modifier = Modifier.size(AccountDimens.avatarBadgeIcon),
                tint = colors.iconOnBrand,
            )
        }
    }
}

@Composable
private fun PlanChip(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPremium) {
        GotestChip(
            text = stringResource(R.string.account_profile_plan_premium),
            modifier = modifier,
            tone = GotestChipTone.Yellow,
            leadingIcon = GotestIcons.Bold.Award,
        )
    } else {
        GotestChip(
            text = stringResource(R.string.account_profile_plan_free),
            modifier = modifier,
            tone = GotestChipTone.Blue,
        )
    }
}

@Composable
private fun RowScope.StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Column(
        modifier =
            modifier
                .weight(1f)
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.medium)
                .padding(horizontal = spacing.sm, vertical = spacing.sm + spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = GotestTheme.colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = GotestTheme.colors.textSecondary)
    }
}

/**
 * Dark "upgrade" banner. Free Trial shows the days left and an Upgrade button; Premium shows the
 * active plan and opens Langganan when tapped.
 */
@Composable
fun PlanBanner(
    subscription: Subscription,
    daysRemaining: Long,
    onUpgrade: () -> Unit,
    onOpenSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = GotestTheme.shapes.largeIncreased
    val color = GotestTheme.colors.bgBrandDark
    if (subscription.isPremium) {
        Surface(onClick = onOpenSubscription, modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            PlanBannerContent(subscription = subscription, daysRemaining = daysRemaining)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color) {
            PlanBannerContent(subscription = subscription, daysRemaining = daysRemaining) {
                GotestButton(
                    text = stringResource(R.string.account_profile_upgrade),
                    onClick = onUpgrade,
                    variant = GotestButtonVariant.Plain,
                    size = GotestButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun PlanBannerContent(
    subscription: Subscription,
    daysRemaining: Long,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Row(
        modifier = modifier.padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs + spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(GotestIcons.Bold.Award),
                    contentDescription = null,
                    modifier = Modifier.size(AccountDimens.iconSmall),
                    tint = colors.bgYellow,
                )
                Text(
                    text = subscription.planName,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textOnBrand,
                )
            }
            Text(
                text = planStatus(subscription, daysRemaining),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textOnBrand,
            )
        }
        action()
    }
}

@Composable
private fun planStatus(
    subscription: Subscription,
    daysRemaining: Long,
): String {
    val until = GotestFormat.mediumDate(subscription.activeUntil)
    return when {
        subscription.isPremium -> {
            stringResource(R.string.account_active_until, until)
        }

        daysRemaining < 0 -> {
            stringResource(R.string.account_profile_trial_ended, until)
        }

        else -> {
            val days = daysRemaining.toInt()
            pluralStringResource(R.plurals.account_profile_trial_remaining, days, days, until)
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun ProfileCardPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.lg),
        ) {
            ProfileCard(profile = AccountPreviewData.profile, onAvatarClick = {})
            PlanBanner(
                subscription = AccountPreviewData.trialSubscription,
                daysRemaining = 30,
                onUpgrade = {},
                onOpenSubscription = {},
            )
            PlanBanner(
                subscription = AccountPreviewData.premiumSubscription,
                daysRemaining = 365,
                onUpgrade = {},
                onOpenSubscription = {},
            )
        }
    }
}
