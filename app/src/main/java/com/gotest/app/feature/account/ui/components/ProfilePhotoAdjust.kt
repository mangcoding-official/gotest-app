package com.gotest.app.feature.account.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.gotest.app.R
import com.gotest.app.core.designsystem.component.GotestButton
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.core.designsystem.theme.GotestTheme
import com.gotest.app.feature.account.domain.PhotoCrop

/** 1 = the photo's short side just covers the circle; the photo opens covering the whole square. */
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val INITIAL_ZOOM = 1f / PhotoCaptureDimens.CROP_CIRCLE_FRACTION
private const val TURNS_PER_CIRCLE = 4
private const val DEGREES_PER_TURN = 90f
private const val HALF = 0.5f

/** Long edge the shot is decoded at for the preview, matching the processor's decode. */
private const val DECODE_SIZE_PX = 2048

/**
 * Zoom, pan and quarter turns of the photo in "Sesuaikan Foto". Lengths are fractions of the
 * square crop area, so the state survives rotation and resizing.
 */
@Stable
internal class PhotoCropState(
    zoom: Float = INITIAL_ZOOM,
    offset: Offset = Offset.Zero,
    quarterTurns: Int = 0,
) {
    var zoom by mutableFloatStateOf(zoom)
        private set

    /** Photo centre relative to the crop area centre. */
    var offset by mutableStateOf(offset)
        private set

    var quarterTurns by mutableIntStateOf(quarterTurns)
        private set

    /** The top-bar button: a clockwise quarter turn, re-centred. */
    fun rotate() {
        quarterTurns = (quarterTurns + 1) % TURNS_PER_CIRCLE
        zoom = INITIAL_ZOOM
        offset = Offset.Zero
    }

    /** The photo's on-screen size after turning, in crop-area units. */
    fun displaySize(photoSize: Size): Size {
        val turned = if (quarterTurns % 2 == 0) photoSize else Size(photoSize.height, photoSize.width)
        val scale = PhotoCaptureDimens.CROP_CIRCLE_FRACTION / turned.minDimension * zoom
        return Size(turned.width * scale, turned.height * scale)
    }

    /** Pinch around [centroid] and drag by [pan], keeping the circle covered by the photo. */
    fun transform(
        photoSize: Size,
        centroid: Offset,
        pan: Offset,
        zoomChange: Float,
    ) {
        val newZoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val factor = newZoom / zoom
        zoom = newZoom
        val moved = (offset - centroid) * factor + centroid + pan
        val size = displaySize(photoSize)
        val maxX = ((size.width - PhotoCaptureDimens.CROP_CIRCLE_FRACTION) * HALF).coerceAtLeast(0f)
        val maxY = ((size.height - PhotoCaptureDimens.CROP_CIRCLE_FRACTION) * HALF).coerceAtLeast(0f)
        offset = Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY))
    }

    /** The circle's bounding square as fractions of the turned photo. */
    fun crop(photoSize: Size): PhotoCrop {
        val size = displaySize(photoSize)
        val circle = PhotoCaptureDimens.CROP_CIRCLE_FRACTION
        return PhotoCrop(
            left = HALF - (circle * HALF + offset.x) / size.width,
            top = HALF - (circle * HALF + offset.y) / size.height,
            width = circle / size.width,
            height = circle / size.height,
            quarterTurns = quarterTurns,
        )
    }

    companion object {
        val Saver =
            listSaver<PhotoCropState, Float>(
                save = { listOf(it.zoom, it.offset.x, it.offset.y, it.quarterTurns.toFloat()) },
                restore = { PhotoCropState(it[0], Offset(it[1], it[2]), it[3].toInt()) },
            )
    }
}

/** Frame 22994 for the shot at [photoUri]; "Gunakan Foto" sends the framed square. */
@Composable
internal fun PhotoAdjustStep(
    photoUri: String,
    isApplying: Boolean,
    onEvent: (PhotoCaptureEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request =
        remember(photoUri, context) {
            ImageRequest
                .Builder(context)
                .data(photoUri)
                .size(DECODE_SIZE_PX)
                .build()
        }
    val painter = rememberAsyncImagePainter(request)
    val painterState by painter.state.collectAsStateWithLifecycle()
    val photoSize =
        (painterState as? AsyncImagePainter.State.Success)
            ?.painter
            ?.intrinsicSize
            ?.takeIf { it.isSpecified && it.minDimension > 0f }
    val cropState = rememberSaveable(photoUri, saver = PhotoCropState.Saver) { PhotoCropState() }
    PhotoAdjustContent(
        photo = painter,
        photoSize = photoSize,
        cropState = cropState,
        isApplying = isApplying,
        failedToLoad = painterState is AsyncImagePainter.State.Error,
        onEvent = onEvent,
        modifier = modifier,
    )
}

/** Frame 22994; [photoSize] is null until the photo has loaded. */
@Composable
private fun PhotoAdjustContent(
    photo: Painter,
    photoSize: Size?,
    cropState: PhotoCropState,
    isApplying: Boolean,
    failedToLoad: Boolean,
    onEvent: (PhotoCaptureEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GotestTheme.spacing
    val canEdit = photoSize != null && !isApplying
    Column(modifier = modifier.fillMaxSize().background(PhotoCaptureColors.background)) {
        PhotoCaptureTopBar(
            title = stringResource(R.string.account_adjust_title),
            leading = {
                PhotoCaptureIconButton(
                    icon = GotestIcons.Linear.ArrowLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = { onEvent(PhotoCaptureEvent.Retake) },
                    enabled = !isApplying,
                )
            },
            trailing = {
                PhotoCaptureIconButton(
                    icon = PhotoCaptureIcons.Rotate,
                    contentDescription = stringResource(R.string.account_adjust_rotate_cd),
                    onClick = cropState::rotate,
                    enabled = canEdit,
                )
            },
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.screenHorizontal, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CropArea(
                photo = photo,
                photoSize = photoSize,
                cropState = cropState,
                enabled = canEdit,
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal).fillMaxWidth().aspectRatio(1f),
            )
            Text(
                text =
                    stringResource(
                        if (failedToLoad) R.string.account_adjust_error else R.string.account_adjust_hint,
                    ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenHorizontal),
                style = MaterialTheme.typography.bodyMedium,
                color = PhotoCaptureColors.hint,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = spacing.screenHorizontal,
                        end = spacing.screenHorizontal,
                        top = spacing.lg,
                        bottom = spacing.sm,
                    ),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            PhotoCaptureOutlinedButton(
                text = stringResource(R.string.account_adjust_retake),
                onClick = { onEvent(PhotoCaptureEvent.Retake) },
                modifier = Modifier.weight(1f),
                enabled = !isApplying,
            )
            GotestButton(
                text = stringResource(R.string.account_adjust_use),
                onClick = { photoSize?.let { onEvent(PhotoCaptureEvent.Use(cropState.crop(it))) } },
                modifier = Modifier.weight(1f),
                enabled = canEdit,
                containerColor = PhotoCaptureColors.content,
                contentColor = GotestTheme.colors.textBrandDark,
            )
        }
    }
}

/** "photo": the square crop area; the photo moves under a dimmed mask with a circular window. */
@Composable
private fun CropArea(
    photo: Painter,
    photoSize: Size?,
    cropState: PhotoCropState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.account_adjust_photo_cd)
    BoxWithConstraints(
        modifier =
            modifier
                .clipToBounds()
                .background(PhotoCaptureColors.cropScrim)
                .pointerInput(photoSize, enabled) {
                    if (photoSize == null || !enabled) return@pointerInput
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val side = size.width.toFloat()
                        val center = Offset(size.width * HALF, size.height * HALF)
                        cropState.transform(
                            photoSize = photoSize,
                            centroid = (centroid - center) / side,
                            pan = pan / side,
                            zoomChange = zoomChange,
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        val side = maxWidth
        if (photoSize != null) {
            // Laid out unturned; the layer turns it, so swap the turned size back.
            val shown = cropState.displaySize(photoSize)
            val turnedSideways = cropState.quarterTurns % 2 == 1
            val width = if (turnedSideways) shown.height else shown.width
            val height = if (turnedSideways) shown.width else shown.height
            Image(
                painter = photo,
                contentDescription = description,
                modifier =
                    Modifier
                        .requiredSize(side * width, side * height)
                        .graphicsLayer {
                            val sidePx = side.toPx()
                            translationX = cropState.offset.x * sidePx
                            translationY = cropState.offset.y * sidePx
                            rotationZ = DEGREES_PER_TURN * cropState.quarterTurns
                        },
                contentScale = ContentScale.FillBounds,
            )
        } else {
            // Keeps the painter drawn so Coil loads it; nothing shows until the size is known.
            Image(
                painter = photo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0f),
            )
        }
        CropMask(modifier = Modifier.fillMaxSize())
    }
}

/** Darkens everything outside the circle and outlines it in white. */
@Composable
private fun CropMask(modifier: Modifier = Modifier) {
    val stroke = PhotoCaptureDimens.guideStroke
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val radius = size.minDimension * PhotoCaptureDimens.CROP_CIRCLE_FRACTION * HALF
        drawRect(color = PhotoCaptureColors.cropScrim)
        drawCircle(color = Color.Black, radius = radius, blendMode = BlendMode.Clear)
        drawCircle(
            color = PhotoCaptureColors.content,
            radius = radius - stroke.toPx() * HALF,
            style = Stroke(width = stroke.toPx()),
        )
    }
}

private val previewPhotoSize = Size(width = 300f, height = 400f)

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhotoAdjustContentPreview() {
    GotestTheme {
        PhotoAdjustContent(
            photo = ColorPainter(PhotoCaptureColors.galleryTile),
            photoSize = previewPhotoSize,
            cropState = remember { PhotoCropState() },
            isApplying = false,
            failedToLoad = false,
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun PhotoAdjustContentApplyingPreview() {
    GotestTheme {
        PhotoAdjustContent(
            photo = ColorPainter(PhotoCaptureColors.galleryTile),
            photoSize = previewPhotoSize,
            cropState = remember { PhotoCropState(quarterTurns = 1) },
            isApplying = true,
            failedToLoad = false,
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 440, heightDp = 956)
@Composable
private fun PhotoAdjustContentLoadingPreview() {
    GotestTheme {
        PhotoAdjustContent(
            photo = ColorPainter(PhotoCaptureColors.galleryTile),
            photoSize = null,
            cropState = remember { PhotoCropState() },
            isApplying = false,
            failedToLoad = false,
            onEvent = {},
        )
    }
}
