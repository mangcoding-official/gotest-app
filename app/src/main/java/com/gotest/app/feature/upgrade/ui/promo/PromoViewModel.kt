package com.gotest.app.feature.upgrade.ui.promo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.PlanTier
import com.gotest.app.feature.upgrade.domain.PromoRepository
import com.gotest.app.navigation.PromoDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@HiltViewModel
class PromoViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        promoRepository: PromoRepository,
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val promoId = savedStateHandle.toRoute<PromoDestination>().promoId
        private val refresh = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<PromoUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        promoRepository.observePromo(promoId),
                        subscriptionRepository.observeSubscription(),
                    ) { promo, subscription ->
                        if (promo == null) {
                            PromoUiState.Empty
                        } else {
                            PromoUiState.Success(promo = promo, isPremium = subscription.tier == PlanTier.Premium)
                        }
                    }.onStart { emit(PromoUiState.Loading) }
                        .catch { emit(PromoUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PromoUiState.Loading)

        fun onEvent(event: PromoUiEvent) {
            when (event) {
                PromoUiEvent.Retry -> refresh.update { it + 1 }
            }
        }
    }
