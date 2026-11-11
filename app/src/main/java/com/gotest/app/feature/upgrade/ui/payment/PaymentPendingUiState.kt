package com.gotest.app.feature.upgrade.ui.payment

import androidx.annotation.StringRes
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
    /** "Saya Sudah Membayar". */
    data object ConfirmPayment : PaymentPendingUiEvent

    data object Retry : PaymentPendingUiEvent
}

sealed interface PaymentPendingEffect {
    data class PaymentConfirmed(
        val orderId: String,
    ) : PaymentPendingEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : PaymentPendingEffect
}
