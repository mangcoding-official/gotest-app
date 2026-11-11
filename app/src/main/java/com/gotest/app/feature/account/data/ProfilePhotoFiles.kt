package com.gotest.app.feature.account.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

/**
 * Files behind "Ambil Foto": the camera writes `capture_*.jpg` and the processor writes the
 * finished `photo_*.jpg`, both in the app cache. Unique names keep Coil from showing a stale
 * image for a reused path.
 */
object ProfilePhotoFiles {
    private const val DIRECTORY = "profile_photo"
    private const val CAPTURE_PREFIX = "capture_"
    private const val PHOTO_PREFIX = "photo_"
    private const val EXTENSION = ".jpg"

    fun newCaptureFile(context: Context): File = newFile(context, CAPTURE_PREFIX)

    fun newPhotoFile(context: Context): File = newFile(context, PHOTO_PREFIX)

    /** The URI string stored as the photo (Coil loads `file://` URIs). */
    fun uriOf(file: File): String = Uri.fromFile(file).toString()

    /** The capture file behind [uri], or null when [uri] is anything else (gallery, finished photo). */
    fun captureFileOf(
        context: Context,
        uri: String,
    ): File? {
        val parsed = uri.toUri()
        val path = parsed.path?.takeIf { parsed.scheme == "file" } ?: return null
        val file = File(path)
        return file.takeIf { it.parentFile == directory(context) && it.name.startsWith(CAPTURE_PREFIX) }
    }

    private fun directory(context: Context): File = File(context.cacheDir, DIRECTORY)

    private fun newFile(
        context: Context,
        prefix: String,
    ): File {
        val directory = directory(context).apply { mkdirs() }
        return File(directory, "$prefix${System.currentTimeMillis()}$EXTENSION")
    }
}
