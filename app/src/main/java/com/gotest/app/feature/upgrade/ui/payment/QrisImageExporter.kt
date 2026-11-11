package com.gotest.app.feature.upgrade.ui.payment

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

/** Pixels per QR module in the exported PNG; a QRIS code comes out around 800 px wide. */
private const val EXPORT_MODULE_PX = 16
private const val EXPORT_QUIET_ZONE_MODULES = 4
private const val PNG_QUALITY = 100

/** Folder of `cacheDir` that the FileProvider serves (`res/xml/file_paths.xml`). */
private const val SHARED_DIR = "shared"
private const val SHARED_FILE_NAME = "qris-gotest.png"

/** Writes an order's QR code as a PNG: into a document for "Simpan QR", to the cache for "Bagikan". */
class QrisImageExporter
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /** Writes the PNG into [destination], a document created with the system file picker. */
        suspend fun save(
            payload: String,
            destination: Uri,
        ) = withContext(Dispatchers.IO) {
            val stream = checkNotNull(context.contentResolver.openOutputStream(destination)) { "No stream" }
            stream.use { writePng(payload, it) }
        }

        /** Writes the PNG to `cache/shared/` and returns its FileProvider uri for the share sheet. */
        suspend fun shareableUri(payload: String): Uri =
            withContext(Dispatchers.IO) {
                val file = File(File(context.cacheDir, SHARED_DIR).apply { mkdirs() }, SHARED_FILE_NAME)
                file.outputStream().use { writePng(payload, it) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

        private fun writePng(
            payload: String,
            out: OutputStream,
        ) {
            val bitmap = QrCodeEncoder.bitmap(payload, EXPORT_MODULE_PX, EXPORT_QUIET_ZONE_MODULES)
            try {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)) { "PNG encoding failed" }
            } finally {
                bitmap.recycle()
            }
        }

        companion object {
            /** Suggested name in the "Simpan QR" file picker. */
            const val SAVE_FILE_NAME = SHARED_FILE_NAME
        }
    }
