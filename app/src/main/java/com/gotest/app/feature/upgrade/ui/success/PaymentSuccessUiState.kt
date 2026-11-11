package com.gotest.app.feature.upgrade.ui.success

import java.time.LocalDate

sealed interface PaymentSuccessUiState {
    data object Loading : PaymentSuccessUiState

    data object Error : PaymentSuccessUiState

    data class Success(
        /** "GoTest Premium". */
        val productName: String,
        /** "Langganan Tahunan • 12 Bulan". */
        val productDetail: String,
        val activatedOn: LocalDate?,
        val activeUntil: LocalDate,
        /** "Virtual Account BCA"; null when the subscription has no payment on record. */
        val paymentMethod: String?,
    ) : PaymentSuccessUiState
}

sealed interface PaymentSuccessUiEvent {
    data object Retry : PaymentSuccessUiEvent
}
