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

    /** Deletes a capture the student retook or abandoned; URIs the app did not create are left alone. */
    fun discard(photoUri: String)
}
