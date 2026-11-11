package com.gotest.app.feature.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestPackageCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.ui.GotestFormat
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantSender
import com.gotest.app.feature.assistant.ui.assistant.AssistantPreviewData
import java.time.LocalDate

private const val SECONDS_PER_MINUTE = 60
private const val TYPING_DOTS = 3

/** "Hari ini" pill; other days show "Kemarin" or the date. */
@Composable
internal fun AssistantDayDivider(
    date: LocalDate,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val label =
        when (date) {
            today -> stringResource(R.string.assistant_today)
            today.minusDays(1) -> stringResource(R.string.assistant_yesterday)
            else -> GotestFormat.longDate(date)
        }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            modifier =
                Modifier
                    .background(GotestTheme.colors.bgGrey, GotestTheme.shapes.full)
                    .padding(horizontal = GotestTheme.spacing.md, vertical = GotestTheme.spacing.xs),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

/** One message: an AI bubble with its package cards and action chips, or the student's bubble. */
@Composable
internal fun AssistantMessageItem(
    message: AssistantMessage,
    onPackageClick: (StudyPackage) -> Unit,
    onActionClick: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (message.sender) {
        AssistantSender.Assistant -> {
            AssistantAnswer(
                message = message,
                onPackageClick = onPackageClick,
                onActionClick = onActionClick,
                modifier = modifier,
            )
        }

        AssistantSender.User -> {
            UserMessage(message = message, modifier = modifier)
        }
    }
}

@Composable
private fun AssistantAnswer(
    message: AssistantMessage,
    onPackageClick: (StudyPackage) -> Unit,
    onActionClick: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val indent = Modifier.padding(horizontal = AssistantDimens.attachmentIndent)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AssistantBubble(text = message.text)
        if (message.packages.isNotEmpty()) {
            Column(modifier = indent, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                message.packages.forEach { pkg ->
                    GotestPackageCard(
                        chipText = pkg.examLabel,
                        title = pkg.title,
                        description = pkg.description,
                        progress = null,
                        actionText =
                            stringResource(
                                if (pkg.type == PackageType.Tryout) {
                                    R.string.assistant_start_tryout
                                } else {
                                    R.string.assistant_start_practice
                                },
                            ),
                        onAction = { onPackageClick(pkg) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (message.actions.isNotEmpty()) {
            FlowRow(
                modifier = indent,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                message.actions.forEach { action ->
                    AssistantPromptChip(text = action.label(), onClick = { onActionClick(action) })
                }
            }
        }
    }
}

@Composable
private fun AssistantAction.label(): String =
    stringResource(
        when (this) {
            is AssistantAction.StartTryout -> R.string.assistant_action_start_tryout
            AssistantAction.BrowseTryouts -> R.string.assistant_action_browse_tryouts
            AssistantAction.OpenLeaderboard -> R.string.assistant_action_leaderboard
        },
    )

/** White AI bubble ("Sender=AI"), square top-left corner next to the avatar. */
@Composable
private fun AssistantBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large.copy(topStart = MaterialTheme.shapes.extraSmall.topStart)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        AssistantAvatar()
        Surface(
            modifier = Modifier.weight(1f),
            shape = shape,
            color = GotestTheme.colors.bgApp,
            border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = GotestTheme.spacing.lg, vertical = GotestTheme.spacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textPrimary,
            )
        }
    }
}

/**
 * Dark brand bubble ("Sender=User"), with the "Pesan suara" label for dictated messages and the
 * attachments above it. A message of attachments only has no bubble.
 */
@Composable
private fun UserMessage(
    message: AssistantMessage,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val shape = MaterialTheme.shapes.large.copy(bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        horizontalAlignment = Alignment.End,
    ) {
        message.voiceDurationSeconds?.let { seconds -> VoiceMessageLabel(seconds = seconds) }
        if (message.attachments.isNotEmpty()) {
            // Same insets as the bubble, so both share a right edge.
            SentAttachments(
                attachments = message.attachments,
                modifier = Modifier.padding(horizontal = AssistantDimens.userBubbleInset),
            )
        }
        if (message.text.isNotEmpty()) {
            Text(
                text = message.text,
                modifier =
                    Modifier
                        .padding(horizontal = AssistantDimens.userBubbleInset)
                        .background(GotestTheme.colors.bgBrandDark, shape)
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = GotestTheme.colors.textOnBrand,
            )
        }
    }
}

@Composable
private fun VoiceMessageLabel(
    seconds: Int,
    modifier: Modifier = Modifier,
) {
    val duration =
        stringResource(R.string.assistant_voice_duration, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.Microphone),
            contentDescription = null,
            modifier = Modifier.size(GotestTheme.sizes.iconXs),
            tint = GotestTheme.colors.iconDefault,
        )
        Text(
            text = stringResource(R.string.assistant_voice_message, duration),
            style = MaterialTheme.typography.labelSmall,
            color = GotestTheme.colors.textSecondary,
        )
    }
}

/** "mengetik": the avatar and three pulsing dots while the assistant answers. */
@Composable
internal fun AssistantTypingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.assistant_typing)
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TYPING_DOTS.toFloat(),
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = AssistantDimens.TYPING_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "typingPhase",
    )
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.sm),
    ) {
        AssistantAvatar()
        Row(
            modifier =
                Modifier
                    .background(GotestTheme.colors.bgApp, MaterialTheme.shapes.large)
                    .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.large)
                    .padding(
                        horizontal = GotestTheme.spacing.lg,
                        vertical = AssistantDimens.typingBubblePaddingVertical,
                    ),
            horizontalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs),
        ) {
            repeat(TYPING_DOTS) { index ->
                Box(
                    modifier =
                        Modifier
                            .size(AssistantDimens.typingDot)
                            // Read in the draw phase so the animation doesn't recompose the row.
                            .graphicsLayer {
                                alpha = if (phase.toInt() == index) 1f else AssistantDimens.TYPING_MIN_ALPHA
                            }.background(GotestTheme.colors.bgBrand, GotestTheme.shapes.full),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssistantChatItemsPreview() {
    val now = System.currentTimeMillis()
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            AssistantDayDivider(date = LocalDate.now())
            AssistantMessageItem(
                message =
                    AssistantMessage(
                        id = "1",
                        sender = AssistantSender.Assistant,
                        text = "Ada! Simulasi UTBK-SNBT Session 1 tersedia sampai 3 hari lagi. Mau langsung mulai?",
                        sentAtEpochMillis = now,
                        actions = listOf(AssistantAction.StartTryout("pkg"), AssistantAction.BrowseTryouts),
                    ),
                onPackageClick = {},
                onActionClick = {},
            )
            AssistantMessageItem(
                message =
                    AssistantMessage(
                        id = "2",
                        sender = AssistantSender.User,
                        text = "Ada tryout UTBK minggu ini?",
                        sentAtEpochMillis = now,
                        voiceDurationSeconds = 4,
                    ),
                onPackageClick = {},
                onActionClick = {},
            )
            AssistantMessageItem(
                message =
                    AssistantMessage(
                        id = "3",
                        sender = AssistantSender.User,
                        text = "Tolong bahas soal nomor 4 ya",
                        sentAtEpochMillis = now,
                        attachments = AssistantPreviewData.attachments,
                    ),
                onPackageClick = {},
                onActionClick = {},
            )
            AssistantTypingIndicator()
        }
    }
}
