package com.gotest.app.feature.upgrade.ui.checkout

import androidx.annotation.StringRes
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.model.Plan

sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState

    data object Error : CheckoutUiState

    data class Success(
        val fullName: String,
        val email: String,
        val plan: Plan,
        val selectedMethod: PaymentMethodType,
        val feeRupiah: Long,
        val totalRupiah: Long,
        val isSubmitting: Boolean,
    ) : CheckoutUiState
}

sealed interface CheckoutUiEvent {
    data class SelectMethod(
        val method: PaymentMethodType,
    ) : CheckoutUiEvent

    data object Pay : CheckoutUiEvent

    data object Retry : CheckoutUiEvent
}

sealed interface CheckoutEffect {
    data class OpenPayment(
        val orderId: String,
    ) : CheckoutEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : CheckoutEffect
}
