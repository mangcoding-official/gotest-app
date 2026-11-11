package com.gotest.app.feature.upgrade.ui.success

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.Subscription
import com.gotest.app.navigation.PaymentSuccessDestination
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
class PaymentSuccessViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val orderId = savedStateHandle.toRoute<PaymentSuccessDestination>().orderId
        private val refresh = MutableStateFlow(0)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<PaymentSuccessUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        subscriptionRepository.observeOrder(orderId),
                        subscriptionRepository.observeSubscription(),
                        ::toUiState,
                    ).onStart { emit(PaymentSuccessUiState.Loading) }
                        .catch { emit(PaymentSuccessUiState.Error) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    PaymentSuccessUiState.Loading,
                )

        fun onEvent(event: PaymentSuccessUiEvent) {
            when (event) {
                PaymentSuccessUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private fun toUiState(
            order: Order,
            subscription: Subscription,
        ): PaymentSuccessUiState =
            PaymentSuccessUiState.Success(
                productName = order.planName,
                productDetail = order.planDetail,
                activatedOn = subscription.activatedOn,
                activeUntil = subscription.activeUntil,
                paymentMethod = subscription.paymentMethod,
            )
    }
