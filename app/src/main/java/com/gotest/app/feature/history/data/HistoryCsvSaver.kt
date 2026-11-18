package com.gotest.app.feature.history.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Where "Export data" puts the CSV; an interface so Riwayat runs in unit tests. */
interface HistoryCsvStore {
    suspend fun save(csv: InputStream): Result<Uri>
}

/**
 * Saves `riwayat-pengerjaan.csv` to Downloads (API-K10 A: "unduhan disimpan ke folder Unduhan dengan
 * pilihan Bagikan"). Android 10 and later: the shared Downloads through MediaStore, no permission.
 * Older phones: the app's own Downloads folder, also without a permission. Returns a shareable Uri.
 */
@Singleton
class HistoryCsvSaver
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : HistoryCsvStore {
        override suspend fun save(csv: InputStream): Result<Uri> =
            withContext(Dispatchers.IO) {
                try {
                    csv.use { input ->
                        Result.success(
                            if (Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q
                            ) {
                                toMediaStore(input)
                            } else {
                                toAppFolder(input)
                            },
                        )
                    }
                } catch (error: IOException) {
                    Result.failure(error)
                } catch (error: SecurityException) {
                    Result.failure(error)
                }
            }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun toMediaStore(input: InputStream): Uri {
            val resolver = context.contentResolver
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, MIME)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val uri =
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("No Downloads")
            // A half-written, still pending file must not stay in Downloads (A2e review).
            val written = runCatching { resolver.openOutputStream(uri)?.use { input.copyTo(it) } }.getOrNull()
            if (written == null) {
                resolver.delete(uri, null, null)
                throw IOException("Could not write $uri")
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }

        private fun toAppFolder(input: InputStream): Uri {
            val folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: throw IOException("No storage")
            val file = File(folder, FILE_NAME)
            file.outputStream().use { input.copyTo(it) }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        companion object {
            const val FILE_NAME = "riwayat-pengerjaan.csv"
            const val MIME = "text/csv"
        }
    }
