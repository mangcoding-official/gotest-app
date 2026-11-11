package com.gotest.app.feature.assistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.assistant.domain.AssistantAttachment
import com.gotest.app.feature.assistant.ui.assistant.AssistantPreviewData

// Attachments have no design yet: these are built from the chat's own parts (hairline cards,
// brand-soft icon squares, the close-circle icon) until one exists.

/** A file waiting in the composer: thumbnail or PDF icon, name and a remove button. */
@Composable
internal fun PendingAttachmentChip(
    attachment: AssistantAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = GotestTheme.colors.bgSubtle) {
        Row(
            modifier = Modifier.padding(start = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentThumbnail(
                attachment = attachment,
                size = AssistantDimens.pendingAttachmentThumbnail,
                shape = MaterialTheme.shapes.small,
            )
            Text(
                text = attachment.name,
                modifier = Modifier.widthIn(max = AssistantDimens.pendingAttachmentNameMaxWidth),
                style = MaterialTheme.typography.labelMedium,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            Icon(
                painter = painterResource(GotestIcons.Linear.CloseCircle),
                contentDescription = stringResource(R.string.assistant_attachment_remove, attachment.name),
                modifier =
                    Modifier
                        .clip(GotestTheme.shapes.full)
                        .clickable(role = Role.Button, onClick = onRemove)
                        .minimumInteractiveComponentSize()
                        .size(GotestTheme.sizes.iconMd),
                tint = GotestTheme.colors.iconDefault,
            )
        }
    }
}

/** What the student sent, right-aligned above their bubble: photos as tiles, PDFs as file cards. */
@Composable
internal fun SentAttachments(
    attachments: List<AssistantAttachment>,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        attachments.forEach { attachment ->
            if (attachment.type.isImage) {
                AttachmentThumbnail(
                    attachment = attachment,
                    size = AssistantDimens.sentAttachmentImage,
                    shape = MaterialTheme.shapes.medium,
                    contentDescription = attachment.name,
                )
            } else {
                SentDocument(attachment = attachment)
            }
        }
    }
}

@Composable
private fun SentDocument(
    attachment: AssistantAttachment,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = GotestTheme.colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentThumbnail(
                attachment = attachment,
                size = AssistantDimens.pendingAttachmentThumbnail,
                shape = MaterialTheme.shapes.small,
            )
            Text(
                text = attachment.name,
                modifier = Modifier.widthIn(max = AssistantDimens.sentAttachmentNameMaxWidth),
                style = MaterialTheme.typography.labelMedium,
                color = GotestTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
    }
}

/** The photo itself, or the document icon on brand-soft for a PDF. */
@Composable
private fun AttachmentThumbnail(
    attachment: AssistantAttachment,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = GotestTheme.colors
    if (attachment.type.isImage) {
        AsyncImage(
            model = attachment.uri,
            contentDescription = contentDescription,
            modifier =
                modifier
                    .size(size)
                    .clip(shape)
                    .background(colors.bgGrey)
                    .border(GotestTheme.sizes.strokeThin, colors.borderSubtle, shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.size(size).background(colors.bgBrandSoft, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Linear.DocumentText),
                contentDescription = contentDescription,
                modifier = Modifier.size(GotestTheme.sizes.iconMd),
                tint = colors.iconBrand,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AssistantAttachmentViewsPreview() {
    GotestTheme {
        Column(
            modifier = Modifier.padding(GotestTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.md),
        ) {
            AssistantPreviewData.attachments.forEach { PendingAttachmentChip(attachment = it, onRemove = {}) }
            SentAttachments(attachments = AssistantPreviewData.attachments)
        }
    }
}
