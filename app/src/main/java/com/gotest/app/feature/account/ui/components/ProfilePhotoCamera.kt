package com.gotest.app.feature.account.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.InitializationException
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.takePicture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.account.data.ProfilePhotoFiles
import kotlinx.coroutines.launch
import java.io.File
import androidx.camera.core.Preview as PreviewUseCase

/**
 * "Ambil Foto" from the sheet: checks for a camera, asks for the CAMERA permission when needed,
 * then calls [onOpenCamera]. Refusals are explained in [snackbarHostState] (the sheet's), with a
 * shortcut to the app's system settings once Android no longer shows the prompt. Needs an
 * Activity, so call it from a Route.
 */
@Composable
fun rememberTakePhotoAction(
    snackbarHostState: SnackbarHostState,
    onOpenCamera: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnOpenCamera by rememberUpdatedState(onOpenCamera)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val canAskAgain =
                activity != null &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            when {
                granted -> {
                    currentOnOpenCamera()
                }

                canAskAgain -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(resources.getString(R.string.account_camera_permission_denied))
                    }
                }

                else -> {
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = resources.getString(R.string.account_camera_permission_denied),
                                actionLabel = resources.getString(R.string.account_camera_permission_settings),
                            )
                        if (result == SnackbarResult.ActionPerformed) context.openAppSettings()
                    }
                }
            }
        }
    return remember(launcher, context) {
        {
            when {
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(resources.getString(R.string.account_camera_unavailable))
                    }
                }

                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                    currentOnOpenCamera()
                }

                else -> {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
    runCatching { startActivity(intent) }
}

/** What the camera controls can do right now; everything is off until a camera is bound. */
@Immutable
internal data class CameraControls(
    /** A preview is streaming (until then the design's placeholder guide shows). */
    val isStreaming: Boolean = false,
    val canCapture: Boolean = false,
    /** The open camera has a flash unit (most front cameras do not). */
    val hasFlash: Boolean = false,
    /** The device has both a front and a back camera. */
    val canSwitchCamera: Boolean = false,
)

/** Frame 22963 with a live CameraX preview; the shot is written to the app cache. */
@Composable
internal fun PhotoCameraStep(
    state: PhotoCaptureUiState,
    onEvent: (PhotoCaptureEvent) -> Unit,
    onPickFromGallery: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val showMessage: (Int) -> Unit = { messageRes ->
        scope.launch { snackbarHostState.showSnackbar(resources.getString(messageRes)) }
    }
    val session =
        rememberCameraSession(
            useFrontCamera = state.useFrontCamera,
            flashOn = state.flashOn,
            onUnavailable = { showMessage(R.string.account_camera_error) },
        )
    var isCapturing by remember { mutableStateOf(false) }
    PhotoCameraContent(
        flashOn = state.flashOn,
        controls =
            CameraControls(
                isStreaming = session.surfaceRequest != null,
                canCapture = session.isReady && !isCapturing,
                hasFlash = session.hasFlashUnit,
                canSwitchCamera = session.canSwitchCamera,
            ),
        onEvent = onEvent,
        onCapture = {
            isCapturing = true
            scope.launch {
                val file = ProfilePhotoFiles.newCaptureFile(context)
                if (session.takePicture(file)) {
                    onEvent(PhotoCaptureEvent.Captured(ProfilePhotoFiles.uriOf(file)))
                } else {
                    file.delete()
                    showMessage(R.string.account_camera_capture_error)
                }
                isCapturing = false
            }
        },
        onPickFromGallery = onPickFromGallery,
        modifier = modifier,
        viewfinder = {
            session.surfaceRequest?.let { request ->
                CameraXViewfinder(surfaceRequest = request, modifier = Modifier.fillMaxSize())
            }
        },
    )
}

/** CameraX use cases bound to the screen's lifecycle, plus what the controls need to know. */
@Stable
private class CameraSession {
    val preview: PreviewUseCase = PreviewUseCase.Builder().build()
    val imageCapture: ImageCapture =
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

    var surfaceRequest by mutableStateOf<SurfaceRequest?>(null)
    var isReady by mutableStateOf(false)
    var hasFlashUnit by mutableStateOf(false)
    var canSwitchCamera by mutableStateOf(false)

    /** False when CameraX could not take or save the shot. */
    suspend fun takePicture(file: File): Boolean =
        try {
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build())
            true
        } catch (ignored: ImageCaptureException) {
            false
        }
}

/**
 * Binds preview and capture to the current lifecycle, rebinding when the lens changes. Uses
 * the back camera when there is no front one (and the reverse); [onUnavailable] reports a
 * device where neither can be opened.
 */
@Composable
private fun rememberCameraSession(
    useFrontCamera: Boolean,
    flashOn: Boolean,
    onUnavailable: () -> Unit,
): CameraSession {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val session = remember { CameraSession() }
    val currentUseFrontCamera by rememberUpdatedState(useFrontCamera)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    LaunchedEffect(session, lifecycleOwner) {
        session.preview.setSurfaceProvider { request -> session.surfaceRequest = request }
        val provider =
            try {
                ProcessCameraProvider.awaitInstance(context)
            } catch (ignored: InitializationException) {
                currentOnUnavailable()
                return@LaunchedEffect
            }
        val hasFront = provider.hasCameraSafely(CameraSelector.DEFAULT_FRONT_CAMERA)
        val hasBack = provider.hasCameraSafely(CameraSelector.DEFAULT_BACK_CAMERA)
        try {
            snapshotFlow { currentUseFrontCamera }.collect { front ->
                session.isReady = false
                provider.unbind(session.preview, session.imageCapture)
                val selector =
                    when {
                        front && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                        hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                        hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> null
                    }
                val camera: Camera? =
                    selector?.let {
                        runCatching {
                            provider.bindToLifecycle(lifecycleOwner, it, session.preview, session.imageCapture)
                        }.getOrNull()
                    }
                if (camera == null) {
                    currentOnUnavailable()
                } else {
                    session.hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                    session.canSwitchCamera = hasFront && hasBack
                    session.isReady = true
                }
            }
        } finally {
            session.isReady = false
            provider.unbind(session.preview, session.imageCapture)
        }
    }
    SideEffect {
        session.imageCapture.flashMode =
            if (flashOn && session.hasFlashUnit) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }
    return session
}

private fun ProcessCameraProvider.hasCameraSafely(selector: CameraSelector): Boolean =
    runCatching { hasCamera(selector) }.getOrDefault(false)

/** Frame 22963 without CameraX: [viewfinder] draws the live preview behind the face guide. */
@Composable
private fun PhotoCameraContent(
    flashOn: Boolean,
    controls: CameraControls,
    onEvent: (PhotoCaptureEvent) -> Unit,
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
    viewfinder: @Composable () -> Unit = {},
) {
    val flashShownOn = flashOn && controls.hasFlash
    Column(modifier = modifier.fillMaxSize().background(PhotoCaptureColors.background)) {
        PhotoCaptureTopBar(
            title = stringResource(R.string.account_camera_title),
            leading = {
                PhotoCaptureIconButton(
                    icon = PhotoCaptureIcons.Close,
                    contentDescription = stringResource(R.string.account_camera_close_cd),
                    onClick = { onEvent(PhotoCaptureEvent.Close) },
                )
            },
            trailing = {
                PhotoCaptureIconButton(
                    icon = if (flashShownOn) PhotoCaptureIcons.Flash else PhotoCaptureIcons.FlashOff,
                    contentDescription =
                        stringResource(
                            if (flashShownOn) {
                                R.string.account_camera_flash_off_cd
                            } else {
                                R.string.account_camera_flash_on_cd
                            },
                        ),
                    onClick = { onEvent(PhotoCaptureEvent.ToggleFlash) },
                    enabled = controls.hasFlash,
                )
            },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            viewfinder()
            Column(
                verticalArrangement = Arrangement.spacedBy(GotestTheme.spacing.screenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FaceGuide(showPlaceholder = !controls.isStreaming)
                Text(
                    text = stringResource(R.string.account_camera_hint),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PhotoCaptureColors.hint,
                    textAlign = TextAlign.Center,
                )
            }
        }
        CameraControlsRow(
            controls = controls,
            onCapture = onCapture,
            onPickFromGallery = onPickFromGallery,
            onSwitchCamera = { onEvent(PhotoCaptureEvent.SwitchCamera) },
        )
    }
}

/** 280dp ring; until the preview streams it also shows the design's faint fill and silhouette. */
@Composable
private fun FaceGuide(
    showPlaceholder: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = GotestTheme.shapes.full
    Box(
        modifier =
            modifier
                .size(PhotoCaptureDimens.faceGuide)
                .then(if (showPlaceholder) Modifier.background(PhotoCaptureColors.guideFill, shape) else Modifier)
                .border(PhotoCaptureDimens.guideStroke, PhotoCaptureColors.content, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Icon(
                painter = painterResource(PhotoCaptureIcons.FaceGuide),
                contentDescription = null,
                modifier = Modifier.size(PhotoCaptureDimens.faceGuideIcon),
                tint = PhotoCaptureColors.faceGuide,
            )
        }
    }
}

/** "controls": gallery shortcut, shutter and front/back switch. */
@Composable
private fun CameraControlsRow(
    controls: CameraControls,
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = PhotoCaptureDimens.controlsPaddingHorizontal,
                    end = PhotoCaptureDimens.controlsPaddingHorizontal,
                    top = spacing.lg,
                    bottom = spacing.xl,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GalleryButton(onClick = onPickFromGallery)
        ShutterButton(onClick = onCapture, enabled = controls.canCapture)
        PhotoCaptureIconButton(
            icon = PhotoCaptureIcons.SwitchCamera,
            contentDescription = stringResource(R.string.account_camera_switch_cd),
            onClick = onSwitchCamera,
            enabled = controls.canSwitchCamera,
            size = PhotoCaptureDimens.sideButton,
            iconSize = PhotoCaptureDimens.switchIcon,
        )
    }
}

/**
 * The design's lavender thumbnail. Showing the latest gallery photo would need storage access,
 * so it carries the gallery glyph instead and opens the same picker as "Pilih dari Galeri".
 */
@Composable
private fun GalleryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PhotoCaptureDimens.galleryRadius)
    Box(
        modifier =
            modifier
                .size(PhotoCaptureDimens.sideButton)
                .clip(shape)
                .background(PhotoCaptureColors.galleryTile)
                .border(PhotoCaptureDimens.galleryBorder, PhotoCaptureColors.content, shape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(GotestIcons.Linear.Gallery),
            contentDescription = stringResource(R.string.account_camera_gallery_cd),
            modifier = Modifier.size(GotestTheme.sizes.iconMd),
            tint = GotestTheme.colors.iconBrandDark,
        )
    }
}

/** 76dp white ring around a 60dp white disc. */
@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = GotestTheme.shapes.full
    val description = stringResource(R.string.account_camera_shutter_cd)
    Box(
        modifier =
            modifier
                .size(PhotoCaptureDimens.shutter)
                .alpha(if (enabled) 1f else PhotoCaptureColors.DISABLED_ALPHA)
                .clip(shape)
                .border(PhotoCaptureDimens.shutterRing, PhotoCaptureColors.content, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .padding(PhotoCaptureDimens.shutterGap)
                .background(PhotoCaptureColors.content, shape),
    )
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhotoCameraContentPreview() {
    GotestTheme {
        PhotoCameraContent(
            flashOn = false,
            controls = CameraControls(),
            onEvent = {},
            onCapture = {},
            onPickFromGallery = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PhotoCameraContentStreamingPreview() {
    GotestTheme {
        PhotoCameraContent(
            flashOn = true,
            controls = CameraControls(isStreaming = true, canCapture = true, hasFlash = true, canSwitchCamera = true),
            onEvent = {},
            onCapture = {},
            onPickFromGallery = {},
            viewfinder = { Box(modifier = Modifier.fillMaxSize().background(GotestTheme.colors.illustrationGrey)) },
        )
    }
}
