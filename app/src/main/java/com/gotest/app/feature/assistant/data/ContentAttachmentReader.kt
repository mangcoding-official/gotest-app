package com.gotest.app.feature.assistant.data

import android.content.ContentResolver
import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.gotest.app.feature.assistant.domain.AttachmentReader
import com.gotest.app.feature.assistant.domain.PickedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Asks the [ContentResolver] for the display name, MIME type and size of a picked file. */
class ContentAttachmentReader
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AttachmentReader {
        private val resolver: ContentResolver = context.contentResolver

        override suspend fun read(uri: String): PickedFile? =
            withContext(Dispatchers.IO) {
                val contentUri = uri.toUri()
                // A revoked grant or a deleted file throws; the file is then simply not attached.
                runCatching {
                    resolver
                        .query(contentUri, COLUMNS, null, null, null)
                        ?.use { cursor ->
                            if (!cursor.moveToFirst()) return@use null
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            val hasName = nameIndex >= 0 && !cursor.isNull(nameIndex)
                            val hasSize = sizeIndex >= 0 && !cursor.isNull(sizeIndex)
                            PickedFile(
                                uri = uri,
                                name =
                                    if (hasName) cursor.getString(nameIndex) else contentUri.lastPathSegment.orEmpty(),
                                mimeType = resolver.getType(contentUri),
                                sizeBytes = if (hasSize) cursor.getLong(sizeIndex) else null,
                            )
                        }
                }.getOrNull()
            }

        private companion object {
            val COLUMNS = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        }
    }
