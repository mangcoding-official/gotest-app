package com.gotest.app.feature.account.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.gotest.app.R
import com.gotest.app.feature.account.domain.PhotoCrop
import com.gotest.app.feature.account.domain.ProfilePhotoProcessor
import com.gotest.app.feature.account.ui.components.PhotoRejected
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

/** "500 × 500 px" from the sheet's hint. */
private const val PHOTO_SIZE_PX = 500

/** Long edge the capture is decoded at; plenty for a 500px crop even when zoomed in. */
private const val DECODE_SIZE_PX = 2048
private const val JPEG_QUALITY = 90

/** The web sends a 512 px JPEG (cropImage.ts). */
private const val UPLOAD_SIZE_PX = 512
private const val UPLOAD_MAX_BYTES = 2L * 1024 * 1024
private val UPLOAD_TYPES = setOf("image/jpeg", "image/png", "image/webp")
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

        override suspend fun uploadJpeg(photoUri: String): Result<ByteArray> {
            // The type and size are read from the ContentResolver, off the main thread.
            withContext(Dispatchers.IO) { rejection(photoUri) }?.let { return Result.failure(PhotoRejected(it)) }
            val source = decode(photoUri)
            return withContext(Dispatchers.IO) {
                runCatching {
                    checkNotNull(source) { "could not decode $photoUri" }
                    val side = min(source.width, source.height).toFloat()
                    val centre =
                        PhotoCrop(
                            left = (source.width - side) / 2f / source.width,
                            top = (source.height - side) / 2f / source.height,
                            width = side / source.width,
                            height = side / source.height,
                            quarterTurns = 0,
                        )
                    val out = ByteArrayOutputStream()
                    source.squareOf(centre, UPLOAD_SIZE_PX).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    out.toByteArray()
                }
            }
        }

        /** Web UpdateAvatarModal: a picked file must be JPG, PNG or WEBP and at most 2 MB. App captures pass. */
        private fun rejection(photoUri: String): String? {
            val uri = photoUri.toUri()
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
            val resolver = context.contentResolver
            val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: 0L
            return when {
                resolver.getType(uri) !in UPLOAD_TYPES -> context.getString(R.string.account_photo_type_error)
                size > UPLOAD_MAX_BYTES -> context.getString(R.string.account_photo_size_error)
                else -> null
            }
        }

        private suspend fun decode(photoUri: String): Bitmap? {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(photoUri)
                    .size(DECODE_SIZE_PX)
                    .allowHardware(false)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
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

/** Cuts the crop square out and scales it to [size] px in one pass. */
private fun Bitmap.squareOf(
    crop: PhotoCrop,
    size: Int = PHOTO_SIZE_PX,
): Bitmap {
    val maxSide = min(width, height)
    val side = min(crop.width * width, crop.height * height).roundToInt().coerceIn(1, maxSide)
    val x = (crop.left * width).roundToInt().coerceIn(0, width - side)
    val y = (crop.top * height).roundToInt().coerceIn(0, height - side)
    val scale = size.toFloat() / side
    val matrix = Matrix().apply { setScale(scale, scale) }
    return Bitmap.createBitmap(this, x, y, side, side, matrix, true)
}
