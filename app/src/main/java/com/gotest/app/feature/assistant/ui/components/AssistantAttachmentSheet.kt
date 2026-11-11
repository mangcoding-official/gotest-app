package com.gotest.app.feature.assistant.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.component.GotestCard
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.assistant.ui.assistant.AttachmentSource
import kotlinx.coroutines.launch

/**
 * "AI Chat / Tambah Lampiran (Sheet)" (frame 22009). A tile slides the sheet away before
 * [onSourceClick] hands over to the camera or a picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AssistantAttachmentSheet(
    onSourceClick: (AttachmentSource) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope
            .launch { sheetState.hide() }
            .invokeOnCompletion { if (!sheetState.isVisible) action() }
    }
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier, sheetState = sheetState) {
        AttachmentSheetContent(
            onSourceClick = { source ->
                hideThen {
                    onDismiss()
                    onSourceClick(source)
                }
            },
            onCancel = { hideThen(onDismiss) },
        )
    }
}

/** Sheet body without the modal window, so previews can render it. */
@Composable
internal fun AttachmentSheetContent(
    onSourceClick: (AttachmentSource) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val colors = GotestTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.assistant_attachment_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.assistant_attachment_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            AttachmentSource.entries.forEach { source ->
                SourceTile(
                    source = source,
                    onClick = { onSourceClick(source) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.bgSubtle, MaterialTheme.shapes.medium)
                    .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(GotestIcons.Linear.InfoCircle),
                contentDescription = null,
                modifier = Modifier.size(AssistantDimens.smallIcon),
                tint = colors.iconDefault,
            )
            Text(
                text = stringResource(R.string.assistant_attachment_hint),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        GotestButton(
            text = stringResource(R.string.assistant_attachment_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            variant = GotestButtonVariant.Plain,
        )
    }
}

/** Kamera / Galeri / Dokumen: a white card with the icon on a brand-soft square. */
@Composable
private fun SourceTile(
    source: AttachmentSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    GotestCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.lg),
        verticalSpacing = spacing.sm,
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(AssistantDimens.attachmentTileIconBox)
                    .background(
                        GotestTheme.colors.bgBrandSoft,
                        RoundedCornerShape(AssistantDimens.attachmentTileIconRadius),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(source.icon),
                contentDescription = null,
                modifier = Modifier.size(AssistantDimens.attachmentTileIcon),
                tint = GotestTheme.colors.iconDefault,
            )
        }
        Text(
            text = stringResource(source.label),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            color = GotestTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@get:DrawableRes
private val AttachmentSource.icon: Int
    get() =
        when (this) {
            AttachmentSource.Camera -> GotestIcons.Linear.Camera
            AttachmentSource.Gallery -> GotestIcons.Linear.Gallery
            AttachmentSource.Document -> GotestIcons.Linear.DocumentText
        }

@get:StringRes
private val AttachmentSource.label: Int
    get() =
        when (this) {
            AttachmentSource.Camera -> R.string.assistant_attachment_camera
            AttachmentSource.Gallery -> R.string.assistant_attachment_gallery
            AttachmentSource.Document -> R.string.assistant_attachment_document
        }

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun AttachmentSheetContentPreview() {
    GotestTheme {
        AttachmentSheetContent(onSourceClick = {}, onCancel = {})
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AttachmentSheetContentNarrowPreview() {
    GotestTheme {
        AttachmentSheetContent(onSourceClick = {}, onCancel = {})
    }
}
