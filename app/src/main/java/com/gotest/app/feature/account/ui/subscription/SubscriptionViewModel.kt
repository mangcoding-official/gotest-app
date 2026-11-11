package com.gotest.app.feature.account.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.Subscription
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
import java.time.LocalDate
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SubscriptionViewModel
    @Inject
    constructor(
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)

        val uiState: StateFlow<SubscriptionUiState> =
            retryKey
                .flatMapLatest {
                    combine<Subscription, List<PaymentRecord>, SubscriptionUiState>(
                        subscriptionRepository.observeSubscription(),
                        subscriptionRepository.observePaymentHistory(),
                    ) { subscription, payments ->
                        SubscriptionUiState.Success(
                            subscription = subscription,
                            isActive = !subscription.activeUntil.isBefore(LocalDate.now()),
                            payments = payments,
                        )
                    }.onStart { emit(SubscriptionUiState.Loading) }
                        .catch { emit(SubscriptionUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SubscriptionUiState.Loading)

        fun onEvent(event: SubscriptionUiEvent) {
            when (event) {
                SubscriptionUiEvent.Retry -> retryKey.update { it + 1 }
            }
        }
    }
