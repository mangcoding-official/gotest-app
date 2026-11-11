package com.gotest.app.feature.account.ui.subscription

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.Subscription

sealed interface SubscriptionUiState {
    data object Loading : SubscriptionUiState

    data object Error : SubscriptionUiState

    /** An empty [payments] list shows the "Belum ada pembayaran" block inside the screen. */
    @Immutable
    data class Success(
        val subscription: Subscription,
        /** False once [Subscription.activeUntil] has passed ("Berakhir" chip). */
        val isActive: Boolean,
        val payments: List<PaymentRecord>,
    ) : SubscriptionUiState
}

sealed interface SubscriptionUiEvent {
    data object Retry : SubscriptionUiEvent
}
