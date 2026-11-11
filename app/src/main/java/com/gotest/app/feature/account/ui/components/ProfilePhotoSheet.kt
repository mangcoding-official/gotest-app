package com.gotest.app.feature.account.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestAvatar
import com.gotest.app.core.designsystem.component.GotestBottomSheet
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.component.GotestButtonVariant
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import kotlinx.coroutines.launch

/** Short skeleton line relative to the long one (61 / 95 in the design). */
private const val SKELETON_SHORT_FRACTION = 0.64f

/**
 * The "Foto Profil" bottom sheet (frame 10299). [snackbarHostState] shows messages above the
 * sheet, where a screen-level snackbar would be hidden by the scrim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoSheet(
    currentPhoto: String?,
    state: PhotoSheetUiState,
    snackbarHostState: SnackbarHostState,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    GotestBottomSheet(onDismissRequest = onDismiss, modifier = modifier, sheetState = sheetState) {
        Box {
            ProfilePhotoSheetContent(
                currentPhoto = currentPhoto,
                state = state,
                onTakePhoto = onTakePhoto,
                onPickFromGallery = onPickFromGallery,
                onSave = onSave,
                onClose = {
                    scope
                        .launch { sheetState.hide() }
                        .invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
                },
            )
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Sheet body without the modal window, so previews can render it. */
@Composable
fun ProfilePhotoSheetContent(
    currentPhoto: String?,
    state: PhotoSheetUiState,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
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
                text = stringResource(R.string.account_photo_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.account_photo_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
            )
        }
        PhotoPreview(photo = state.selectedPhotoUri ?: currentPhoto)
        AccountListCard {
            PhotoSourceRow(
                icon = GotestIcons.Linear.Camera,
                label = stringResource(R.string.account_photo_take),
                onClick = onTakePhoto,
            )
            PhotoSourceRow(
                icon = GotestIcons.Linear.Gallery,
                label = stringResource(R.string.account_photo_gallery),
                onClick = onPickFromGallery,
            )
        }
        Text(
            text = stringResource(R.string.account_photo_hint),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GotestButton(
                text = stringResource(R.string.account_photo_close),
                onClick = onClose,
                modifier = Modifier.weight(1f),
                variant = GotestButtonVariant.Plain,
                enabled = !state.isSaving,
            )
            GotestButton(
                text = stringResource(R.string.account_photo_save),
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = state.canSave,
            )
        }
    }
}

/** Grey stage showing the photo in a list-row card and in a profile card. */
@Composable
private fun PhotoPreview(
    photo: String?,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val description = stringResource(R.string.account_photo_preview_cd)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(AccountDimens.photoPreviewHeight)
                .background(GotestTheme.colors.bgSubtle, MaterialTheme.shapes.medium)
                .border(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle, MaterialTheme.shapes.medium)
                .padding(spacing.md)
                .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(spacing.xxl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewCard(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                modifier = Modifier.padding(spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                GotestAvatar(model = photo, contentDescription = null, size = AccountDimens.photoPreviewAvatar)
                SkeletonLines(
                    modifier = Modifier.height(AccountDimens.photoPreviewAvatar),
                    alignment = Alignment.Start,
                )
            }
        }
        PreviewCard {
            Column(
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GotestAvatar(model = photo, contentDescription = null, size = AccountDimens.photoPreviewAvatar)
                SkeletonLines(alignment = Alignment.CenterHorizontally)
            }
        }
    }
}

@Composable
private fun PreviewCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = GotestTheme.colors.bgApp,
        border = BorderStroke(GotestTheme.sizes.strokeThin, GotestTheme.colors.borderSubtle),
        content = content,
    )
}

/** Two grey bars standing in for a name and an email. */
@Composable
private fun SkeletonLines(
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = AccountDimens.skeletonLong),
        verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.xs, Alignment.CenterVertically),
        horizontalAlignment = alignment,
    ) {
        SkeletonLine(height = AccountDimens.skeletonThick, modifier = Modifier.fillMaxWidth())
        SkeletonLine(
            height = AccountDimens.skeletonThin,
            modifier = Modifier.fillMaxWidth(SKELETON_SHORT_FRACTION).widthIn(max = AccountDimens.skeletonShort),
        )
    }
}

@Composable
private fun SkeletonLine(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(height)
                .background(GotestTheme.colors.bgGrey, GotestTheme.shapes.full),
    )
}

@Composable
private fun PhotoSourceRow(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), color = GotestTheme.colors.bgApp) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountIconTile(
                icon = icon,
                tint = GotestTheme.colors.iconDefault,
                containerColor = GotestTheme.colors.bgSubtle,
                size = AccountTileSize.Compact,
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = GotestTheme.colors.textPrimary,
            )
        }
    }
}

/**
 * Opens the system photo picker (images only) and reports the picked content URI. Needs an
 * Activity, so call it from a Route, never from a previewed Screen.
 */
@Composable
fun rememberGalleryPhotoPicker(onPick: (String) -> Unit): () -> Unit {
    val currentOnPick by rememberUpdatedState(onPick)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { currentOnPick(it.toString()) }
        }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun ProfilePhotoSheetContentPreview() {
    GotestTheme {
        ProfilePhotoSheetContent(
            currentPhoto = null,
            state = PhotoSheetUiState(),
            onTakePhoto = {},
            onPickFromGallery = {},
            onSave = {},
            onClose = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfilePhotoSheetContentPickedPreview() {
    GotestTheme {
        ProfilePhotoSheetContent(
            currentPhoto = null,
            state = PhotoSheetUiState(selectedPhotoUri = "content://media/picker/0/1"),
            onTakePhoto = {},
            onPickFromGallery = {},
            onSave = {},
            onClose = {},
        )
    }
}
