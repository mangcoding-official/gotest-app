package com.gotest.app.feature.assistant.domain

/** The file types "Tambah Lampiran" accepts: "JPG, PNG, atau PDF". */
enum class AttachmentType(
    val mimeType: String,
    private val extensions: Set<String>,
) {
    Jpeg("image/jpeg", setOf("jpg", "jpeg")),
    Png("image/png", setOf("png")),
    Pdf("application/pdf", setOf("pdf")),
    ;

    val isImage: Boolean get() = this != Pdf

    companion object {
        /** Matches the MIME type the system reports, or the file extension when it reports none. */
        fun of(
            mimeType: String?,
            fileName: String,
        ): AttachmentType? =
            if (mimeType != null) {
                entries.firstOrNull { it.mimeType == mimeType }
            } else {
                val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
                entries.firstOrNull { extension in it.extensions }
            }
    }
}

/** A photo or document sent with a message. [uri] is a content URI today, a URL once uploaded. */
data class AssistantAttachment(
    val uri: String,
    val name: String,
    val type: AttachmentType,
    val sizeBytes: Long,
)

/** What the system tells us about a file the student picked, before [AttachmentRules] checks it. */
data class PickedFile(
    val uri: String,
    val name: String,
    val mimeType: String?,
    /** Null when the provider doesn't report a size. */
    val sizeBytes: Long?,
)

/** Why a picked file was left out. */
enum class AttachmentRejection { UnsupportedType, TooLarge, TooMany }

data class AttachmentCheck(
    val accepted: List<AssistantAttachment>,
    val rejections: Set<AttachmentRejection>,
)

/** "Maks. 10 MB per file • Maks. 3 file per pesan". */
object AttachmentRules {
    const val MAX_FILES = 3
    const val MAX_FILE_BYTES = 10L * 1024 * 1024

    /**
     * Splits [picked] into what fits next to [attached] and why the rest doesn't. Files already
     * attached are skipped silently; a file with an unknown size is let through.
     */
    fun check(
        picked: List<PickedFile>,
        attached: List<AssistantAttachment>,
    ): AttachmentCheck {
        val accepted = mutableListOf<AssistantAttachment>()
        val rejections = mutableSetOf<AttachmentRejection>()
        val attachedUris = attached.mapTo(mutableSetOf()) { it.uri }
        picked.filter { it.uri !in attachedUris }.distinctBy { it.uri }.forEach { file ->
            val type = AttachmentType.of(file.mimeType, file.name)
            val size = file.sizeBytes ?: 0L
            when {
                type == null -> rejections += AttachmentRejection.UnsupportedType
                size > MAX_FILE_BYTES -> rejections += AttachmentRejection.TooLarge
                attached.size + accepted.size >= MAX_FILES -> rejections += AttachmentRejection.TooMany
                else -> accepted += AssistantAttachment(file.uri, file.name, type, size)
            }
        }
        return AttachmentCheck(accepted, rejections)
    }
}

/** Reads name, type and size of a picked file. */
fun interface AttachmentReader {
    /** Null when the file can no longer be read (revoked permission, deleted file). */
    suspend fun read(uri: String): PickedFile?
}
