package com.gotest.app.feature.home.ui.notification

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestColors
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.home.domain.NotificationType
import com.gotest.app.feature.home.ui.components.HomeIconTile

/** One notification: type badge, title with age and unread dot, and a two-line message. */
@Composable
internal fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    val badge = colors.badgeFor(item.type)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = spacing.lg, vertical = spacing.md + spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        HomeIconTile(icon = badge.icon, containerColor = badge.container, iconColor = badge.content)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.age.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
                if (!item.isRead) {
                    val unread = stringResource(R.string.home_cd_notification_unread)
                    Box(
                        modifier =
                            Modifier
                                .size(GotestTheme.sizes.badgeDot)
                                .background(colors.bgBrand, GotestTheme.shapes.full)
                                .semantics { contentDescription = unread },
                    )
                }
            }
            Text(
                text = item.message,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class NotificationBadge(
    @param:DrawableRes val icon: Int,
    val container: Color,
    val content: Color,
)

private fun GotestColors.badgeFor(type: NotificationType): NotificationBadge =
    when (type) {
        NotificationType.TryoutResult -> NotificationBadge(GotestIcons.Bold.Award, bgGreenSoft, bgGreen)
        NotificationType.NewPackage -> NotificationBadge(GotestIcons.Bold.Note2, bgBlueSoft, bgBlue)
        NotificationType.RankUp -> NotificationBadge(GotestIcons.Bold.Leaderboard, bgBrandSoft, bgBrand)
        NotificationType.TrialEnding -> NotificationBadge(GotestIcons.Bold.InfoCircle, bgYellowSoft, bgYellow)
        NotificationType.PracticeCompleted -> NotificationBadge(GotestIcons.Bold.TickCircle, bgGreenSoft, bgGreen)
    }

@Composable
private fun NotificationAge.label(): String =
    when (this) {
        NotificationAge.JustNow -> stringResource(R.string.home_notification_just_now)
        is NotificationAge.Minutes -> stringResource(R.string.home_notification_minutes_ago, value)
        is NotificationAge.Hours -> stringResource(R.string.home_notification_hours_ago, value)
        is NotificationAge.Days -> stringResource(R.string.home_notification_days_ago, value)
    }

/** Literal sample data for the notification @Preview functions (the design's five notifications). */
internal object NotificationPreviewData {
    private fun item(
        id: String,
        type: NotificationType,
        title: String,
        message: String,
        age: NotificationAge,
        isRead: Boolean,
    ) = NotificationItem(id, type, title, message, age, isRead, targetId = null)

    val success =
        NotificationUiState.Success(
            filter = NotificationFilter.All,
            unreadCount = 2,
            groups =
                listOf(
                    NotificationGroup(
                        day = NotificationDay.Today,
                        items =
                            listOf(
                                item(
                                    id = "1",
                                    type = NotificationType.TryoutResult,
                                    title = "Hasil tryout sudah keluar",
                                    message =
                                        "Skor Simulasi UTBK-SNBT Session 2 kamu 540. " +
                                            "Lihat pembahasannya sekarang.",
                                    age = NotificationAge.Minutes(value = 10),
                                    isRead = false,
                                ),
                                item(
                                    id = "2",
                                    type = NotificationType.NewPackage,
                                    title = "Paket baru tersedia",
                                    message = "Latihan TWK CPNS Session 2 sudah bisa kamu kerjakan.",
                                    age = NotificationAge.Hours(value = 2),
                                    isRead = false,
                                ),
                            ),
                    ),
                    NotificationGroup(
                        day = NotificationDay.Yesterday,
                        items =
                            listOf(
                                item(
                                    id = "3",
                                    type = NotificationType.RankUp,
                                    title = "Peringkatmu naik!",
                                    message = "Kamu naik 1 peringkat ke posisi #12 minggu ini.",
                                    age = NotificationAge.Days(value = 1),
                                    isRead = true,
                                ),
                                item(
                                    id = "4",
                                    type = NotificationType.TrialEnding,
                                    title = "Masa trial akan berakhir",
                                    message = "Paket Free Trial berakhir dalam 30 hari. Upgrade untuk akses penuh.",
                                    age = NotificationAge.Days(value = 1),
                                    isRead = true,
                                ),
                                item(
                                    id = "5",
                                    type = NotificationType.PracticeCompleted,
                                    title = "Latihan selesai",
                                    message = "Kamu menyelesaikan Latihan September Session 1 dengan skor 31.",
                                    age = NotificationAge.Days(value = 1),
                                    isRead = true,
                                ),
                            ),
                    ),
                ),
        )
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun NotificationRowPreview() {
    GotestTheme {
        GotestCard(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            contentPadding = PaddingValues(),
            verticalSpacing = GotestTheme.spacing.none,
        ) {
            NotificationPreviewData.success.groups
                .first()
                .items
                .forEach { NotificationRow(item = it, onClick = {}) }
        }
    }
}
