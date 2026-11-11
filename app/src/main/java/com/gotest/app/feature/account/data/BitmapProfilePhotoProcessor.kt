package com.gotest.app.feature.account.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.gotest.app.feature.account.domain.PhotoCrop
import com.gotest.app.feature.account.domain.ProfilePhotoProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

/** "500 × 500 px" from the sheet's hint. */
private const val PHOTO_SIZE_PX = 500

/** Long edge the capture is decoded at; plenty for a 500px crop even when zoomed in. */
private const val DECODE_SIZE_PX = 2048
private const val JPEG_QUALITY = 90
private const val DEGREES_PER_TURN = 90f
private const val TURNS_PER_CIRCLE = 4

/**
 * Crops on the device until the API takes uploads. Coil decodes the photo so the EXIF
 * orientation matches what "Sesuaikan Foto" showed.
 */
class BitmapProfilePhotoProcessor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ProfilePhotoProcessor {
        override suspend fun crop(
            photoUri: String,
            crop: PhotoCrop,
        ): Result<String> {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(photoUri)
                    .size(DECODE_SIZE_PX)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            val result = SingletonImageLoader.get(context).execute(request)
            val source =
                (result as? SuccessResult)?.image?.toBitmap()
                    ?: return Result.failure(IllegalStateException("could not decode $photoUri"))
            return withContext(Dispatchers.IO) {
                runCatching {
                    val square = source.turned(crop.quarterTurns).squareOf(crop)
                    val file = ProfilePhotoFiles.newPhotoFile(context)
                    FileOutputStream(file).use { square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                    ProfilePhotoFiles.uriOf(file)
                }
            }.onSuccess { discard(photoUri) }
        }

        override fun discard(photoUri: String) {
            ProfilePhotoFiles.captureFileOf(context, photoUri)?.delete()
        }
    }

private fun Bitmap.turned(quarterTurns: Int): Bitmap {
    if (quarterTurns % TURNS_PER_CIRCLE == 0) return this
    val matrix = Matrix().apply { postRotate(DEGREES_PER_TURN * quarterTurns) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/** Cuts the crop square out and scales it to [PHOTO_SIZE_PX] in one pass. */
private fun Bitmap.squareOf(crop: PhotoCrop): Bitmap {
    val maxSide = min(width, height)
    val side = min(crop.width * width, crop.height * height).roundToInt().coerceIn(1, maxSide)
    val x = (crop.left * width).roundToInt().coerceIn(0, width - side)
    val y = (crop.top * height).roundToInt().coerceIn(0, height - side)
    val scale = PHOTO_SIZE_PX.toFloat() / side
    val matrix = Matrix().apply { setScale(scale, scale) }
    return Bitmap.createBitmap(this, x, y, side, side, matrix, true)
}
