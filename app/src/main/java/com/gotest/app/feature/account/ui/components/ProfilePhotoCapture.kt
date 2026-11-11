package com.gotest.app.feature.account.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme

/** The camera flow's own dark palette; frames 22963 and 22994 are the app's only dark screens. */
internal object PhotoCaptureColors {
    val background = Color(0xFF0E0E10)
    val content = Color.White
    val hint = Color.White.copy(alpha = 0.8f)
    val button = Color.White.copy(alpha = 0.12f)
    val outline = Color.White.copy(alpha = 0.4f)
    val guideFill = Color.White.copy(alpha = 0.05f)
    val faceGuide = Color.White.copy(alpha = 0.3f)

    /** Placeholder photo of the gallery button. */
    val galleryTile = Color(0xFFCCC1F0)

    /** Dims the photo outside the crop circle (the design's #3A2F4A over its lavender photo). */
    val cropScrim = Color.Black.copy(alpha = 0.72f)
    const val DISABLED_ALPHA = 0.4f
}

/** Lucide glyphs of the camera frames (not Vuesax, so they stay out of GotestIcons). */
internal object PhotoCaptureIcons {
    @DrawableRes val Close: Int = R.drawable.ic_camera_close

    @DrawableRes val Flash: Int = R.drawable.ic_camera_flash

    @DrawableRes val FlashOff: Int = R.drawable.ic_camera_flash_off

    @DrawableRes val SwitchCamera: Int = R.drawable.ic_camera_switch

    @DrawableRes val Rotate: Int = R.drawable.ic_camera_rotate

    @DrawableRes val FaceGuide: Int = R.drawable.ic_camera_face_guide
}

/** Sizes of the camera flow that have no design token. */
internal object PhotoCaptureDimens {
    /** Close, flash, back and rotate buttons in the top bar. */
    val topButton = 40.dp

    /** Gallery and switch-camera buttons beside the shutter. */
    val sideButton = 48.dp
    val switchIcon = 22.dp
    val galleryRadius = 12.dp
    val galleryBorder = 2.dp
    val shutter = 76.dp
    val shutterRing = 4.dp
    val shutterGap = 8.dp
    val faceGuide = 280.dp
    val faceGuideIcon = 120.dp
    val guideStroke = 2.dp
    val controlsPaddingHorizontal = 40.dp

    /** Bottom row height (16 + 76 + 24), so snackbars clear the shutter. */
    val controlsHeight = 116.dp

    /** Crop circle diameter relative to the square crop area (300 / 400). */
    const val CROP_CIRCLE_FRACTION = 0.75f
}

/**
 * Full-screen "Ambil Foto" (frame 22963) and "Sesuaikan Foto" (frame 22994) over the "Foto
 * Profil" sheet. It is a dialog so it covers the sheet and the floating bottom bar, which live
 * in their own windows. Binds CameraX, so it only runs on a device; the steps have previews.
 */
@Composable
fun ProfilePhotoCaptureDialog(
    state: PhotoCaptureUiState,
    onEvent: (PhotoCaptureEvent) -> Unit,
    onPickFromGallery: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val capturedPhoto = state.capturedPhotoUri
    Dialog(
        onDismissRequest = {
            when {
                state.isApplying -> Unit
                capturedPhoto == null -> onEvent(PhotoCaptureEvent.Close)
                else -> onEvent(PhotoCaptureEvent.Retake)
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        LightSystemBarIcons()
        Box(modifier = modifier.fillMaxSize().background(PhotoCaptureColors.background)) {
            if (capturedPhoto == null) {
                PhotoCameraStep(
                    state = state,
                    onEvent = onEvent,
                    onPickFromGallery = onPickFromGallery,
                    snackbarHostState = snackbarHostState,
                )
            } else {
                PhotoAdjustStep(photoUri = capturedPhoto, isApplying = state.isApplying, onEvent = onEvent)
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = PhotoCaptureDimens.controlsHeight),
            )
        }
    }
}

/** The dialog window draws behind the system bars; its icons have to be light on the dark flow. */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    val window = ((view.parent as? DialogWindowProvider) ?: (view as? DialogWindowProvider))?.window
    SideEffect {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
}

/** "top-bar" of both frames: round button, centred white title, round button. */
@Composable
internal fun PhotoCaptureTopBar(
    title: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = GotestTheme.sizes.appBarHeight)
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = PhotoCaptureColors.content,
            textAlign = TextAlign.Center,
        )
        trailing()
    }
}

/** Translucent white circle holding a white glyph. */
@Composable
internal fun PhotoCaptureIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = PhotoCaptureDimens.topButton,
    iconSize: Dp = GotestTheme.sizes.iconMd,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (enabled) 1f else PhotoCaptureColors.DISABLED_ALPHA),
        enabled = enabled,
        shape = GotestTheme.shapes.full,
        color = PhotoCaptureColors.button,
        contentColor = PhotoCaptureColors.content,
    ) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** "Ambil Ulang": the white-outlined button of frame 22994, sized like a large GotestButton. */
@Composable
internal fun PhotoCaptureOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = GotestTheme.spacing
    Surface(
        onClick = onClick,
        modifier = modifier.alpha(if (enabled) 1f else PhotoCaptureColors.DISABLED_ALPHA),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        contentColor = PhotoCaptureColors.content,
        border = BorderStroke(GotestTheme.sizes.strokeThin, PhotoCaptureColors.outline),
    ) {
        Box(
            modifier = Modifier.heightIn(min = GotestTheme.sizes.buttonLarge).padding(spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = GotestTheme.typography.buttonLarge, textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0E10)
@Composable
private fun PhotoCaptureTopBarPreview() {
    GotestTheme {
        PhotoCaptureTopBar(
            title = "Ambil Foto",
            leading = {
                PhotoCaptureIconButton(icon = PhotoCaptureIcons.Close, contentDescription = "Tutup", onClick = {})
            },
            trailing = {
                PhotoCaptureIconButton(icon = GotestIcons.Linear.ArrowLeft, contentDescription = "", onClick = {})
            },
            modifier = Modifier.background(PhotoCaptureColors.background),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E0E10)
@Composable
private fun PhotoCaptureOutlinedButtonPreview() {
    GotestTheme {
        Box(modifier = Modifier.padding(GotestTheme.spacing.lg)) {
            PhotoCaptureOutlinedButton(text = "Ambil Ulang", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
