package com.gotest.app.feature.account.domain

/**
 * The square a student framed in "Sesuaikan Foto", as fractions of the photo after it is
 * turned clockwise [quarterTurns] times (0 to 3).
 */
data class PhotoCrop(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val quarterTurns: Int,
)

/** Turns a camera capture into the profile photo file. */
interface ProfilePhotoProcessor {
    /** Writes the [crop] of [photoUri] as a 500 × 500 JPEG and returns the new file's URI. */
    suspend fun crop(
        photoUri: String,
        crop: PhotoCrop,
    ): Result<String>

    /**
     * The photo to send to `POST /profil/foto`: the centre square as a 512 × 512 JPEG at quality 0.9, as
     * the web's cropImage. A gallery pick must be JPG, PNG or WEBP of at most 2 MB, as on the web.
     */
    suspend fun uploadJpeg(photoUri: String): Result<ByteArray>

    /** Deletes a capture the student retook or abandoned; URIs the app did not create are left alone. */
    fun discard(photoUri: String)
}
