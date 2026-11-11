package com.gotest.app.feature.upgrade.ui.payment

import android.net.Uri
import androidx.annotation.StringRes
import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.OrderStatus

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L

sealed interface PaymentPendingUiState {
    data object Loading : PaymentPendingUiState

    data object Error : PaymentPendingUiState

    data class Success(
        val order: Order,
        /** Seconds left until [Order.expiresAtEpochMillis], ticking every second. */
        val remainingSeconds: Long,
        val isConfirming: Boolean,
    ) : PaymentPendingUiState {
        val isExpired: Boolean get() = remainingSeconds <= 0L || order.status == OrderStatus.Expired

        val hours: Long get() = remainingSeconds / SECONDS_PER_HOUR

        val minutes: Long get() = remainingSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE

        val seconds: Long get() = remainingSeconds % SECONDS_PER_MINUTE
    }
}

sealed interface PaymentPendingUiEvent {
    /** "Saya Sudah Membayar" (VA, QRIS) or "Cek Status Pembayaran" (e-wallet). */
    data object ConfirmPayment : PaymentPendingUiEvent

    /** "Simpan QR": [destination] is the PNG document the student just created. */
    data class SaveQr(
        val destination: Uri,
    ) : PaymentPendingUiEvent

    /** "Bagikan" on the QRIS card. */
    data object ShareQr : PaymentPendingUiEvent

    data object Retry : PaymentPendingUiEvent
}

sealed interface PaymentPendingEffect {
    data class PaymentConfirmed(
        val orderId: String,
    ) : PaymentPendingEffect

    /** Hands the QR code PNG ([uri], a FileProvider uri) to the share sheet. */
    data class ShareQrImage(
        val uri: Uri,
    ) : PaymentPendingEffect

    /** Opens the wallet app once, when the e-wallet screen first appears. */
    data class OpenWalletApp(
        val wallet: EWallet,
    ) : PaymentPendingEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : PaymentPendingEffect
}
