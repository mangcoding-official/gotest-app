package com.gotest.app.feature.upgrade.ui.promo

import com.gotest.app.feature.upgrade.domain.Promo

sealed interface PromoUiState {
    data object Loading : PromoUiState

    data object Error : PromoUiState

    /** The promo does not exist or has ended. */
    data object Empty : PromoUiState

    data class Success(
        val promo: Promo,
        /** A Premium student cannot take the promo; "Ambil Promo" is disabled. */
        val isPremium: Boolean,
    ) : PromoUiState
}

sealed interface PromoUiEvent {
    data object Retry : PromoUiEvent
}
