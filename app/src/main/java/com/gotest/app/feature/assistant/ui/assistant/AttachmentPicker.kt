package com.gotest.app.feature.assistant.ui.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.gotest.app.R
import com.gotest.app.feature.assistant.domain.AttachmentType
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Folder under cacheDir that res/xml/file_paths.xml shares through the FileProvider. */
private const val SHARED_CACHE_DIRECTORY = "shared"

/** The photo picker only takes a limit above one; a single slot uses the single-photo picker. */
private const val MIN_MULTIPLE_PICK = 2

private val photoNameFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

/**
 * Starts the camera, photo picker or document picker behind the "Tambah Lampiran" tiles and
 * reports the content URIs the student picked. The photo picker lets them pick up to
 * [remainingSlots] photos; type and size are checked afterwards by the ViewModel, since older
 * devices fall back to pickers that ignore the limits. Needs an Activity, so call it from the
 * Route, never from a previewed Screen.
 */
@Composable
internal fun rememberAttachmentPicker(
    remainingSlots: Int,
    onPick: (uris: List<String>) -> Unit,
    onMessage: (messageRes: Int) -> Unit,
): (AttachmentSource) -> Unit {
    val context = LocalContext.current
    val currentOnPick by rememberUpdatedState(onPick)
    val currentOnMessage by rememberUpdatedState(onMessage)
    // Survives the activity being recreated while the camera app is in front.
    var captureUri by rememberSaveable { mutableStateOf<String?>(null) }

    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = captureUri
            captureUri = null
            if (saved && uri != null) currentOnPick(listOf(uri))
        }
    val launchCamera = {
        launchOrReport(R.string.assistant_attachment_camera_unavailable, currentOnMessage) {
            val uri = newCaptureUri(context)
            captureUri = uri.toString()
            camera.launch(uri)
        }
    }
    // CAMERA is declared in the manifest, so the capture intent fails unless it is granted.
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else currentOnMessage(R.string.assistant_attachment_camera_denied)
        }
    val singlePhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { currentOnPick(listOf(it.toString())) }
        }
    val multiplePhotos =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = remainingSlots.coerceAtLeast(MIN_MULTIPLE_PICK)),
        ) { uris ->
            if (uris.isNotEmpty()) currentOnPick(uris.map(Uri::toString))
        }
    val document =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { currentOnPick(listOf(it.toString())) }
        }

    return { source ->
        when (source) {
            AttachmentSource.Camera -> {
                if (context.hasCameraPermission()) {
                    launchCamera()
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            }

            AttachmentSource.Gallery -> {
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (remainingSlots >= MIN_MULTIPLE_PICK) multiplePhotos.launch(request) else singlePhoto.launch(request)
            }

            AttachmentSource.Document -> {
                launchOrReport(R.string.assistant_attachment_document_unavailable, currentOnMessage) {
                    document.launch(AttachmentType.entries.map { it.mimeType }.toTypedArray())
                }
            }
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** An empty file in cacheDir/shared/ for the camera app to write the photo into. */
private fun newCaptureUri(context: Context): Uri {
    val directory = File(context.cacheDir, SHARED_CACHE_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "Foto_${photoNameFormat.format(LocalDateTime.now())}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Some devices have no camera app or no document picker; say so instead of crashing. */
private fun launchOrReport(
    @StringRes unavailableMessage: Int,
    onMessage: (Int) -> Unit,
    launch: () -> Unit,
) {
    if (runCatching(launch).isFailure) onMessage(unavailableMessage)
}
