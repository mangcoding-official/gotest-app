package com.gotest.app.feature.upgrade.ui.checkout

import androidx.annotation.StringRes
import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.model.Plan
import com.gotest.app.feature.upgrade.domain.Promo

sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState

    data object Error : CheckoutUiState

    data class Success(
        val fullName: String,
        val email: String,
        val plan: Plan,
        /** Promo taken on the promo screen; [priceRupiah] is already its price. */
        val promo: Promo?,
        val selectedMethod: PaymentMethodType,
        /** Wallet picked in "Pilih E-Wallet"; GoPay until the student picks another. */
        val selectedWallet: EWallet,
        val isWalletSheetVisible: Boolean,
        /** "Harga Paket": the plan price, or the promo price. */
        val priceRupiah: Long,
        val feeRupiah: Long,
        val totalRupiah: Long,
        val isSubmitting: Boolean,
    ) : CheckoutUiState
}

sealed interface CheckoutUiEvent {
    /** Choosing E-Wallet also opens "Pilih E-Wallet". */
    data class SelectMethod(
        val method: PaymentMethodType,
    ) : CheckoutUiEvent

    data class SelectWallet(
        val wallet: EWallet,
    ) : CheckoutUiEvent

    data object DismissWalletSheet : CheckoutUiEvent

    /** "Bayar Sekarang"; with E-Wallet selected it opens the wallet sheet instead. */
    data object Pay : CheckoutUiEvent

    /** "Bayar dengan <wallet>" in the wallet sheet. */
    data object PayWithWallet : CheckoutUiEvent

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
