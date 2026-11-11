package com.gotest.app.feature.upgrade.ui.payment

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Renders a QRIS payload as a black-on-white QR code with ZXing. */
internal object QrCodeEncoder {
    /**
     * One pixel per module and no quiet zone: small enough to build during composition and
     * scaled up on screen without filtering. The white frame around it is the quiet zone.
     */
    fun screenBitmap(payload: String): Bitmap = bitmap(payload, modulePx = 1, quietZoneModules = 0)

    /**
     * [modulePx] pixels per module, with a [quietZoneModules]-wide white margin so the saved or
     * shared image scans on its own.
     */
    fun bitmap(
        payload: String,
        modulePx: Int,
        quietZoneModules: Int,
    ): Bitmap {
        val hints =
            mapOf(
                EncodeHintType.MARGIN to quietZoneModules,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
            )
        // Zero width and height give the smallest matrix: one entry per module.
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
        val size = matrix.width * modulePx
        val pixels =
            IntArray(size * size) { index ->
                val isDark = matrix[index % size / modulePx, index / size / modulePx]
                if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        return createBitmap(size, size).apply { setPixels(pixels, 0, size, 0, 0, size, size) }
    }
}
