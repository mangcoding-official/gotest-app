package com.gotest.app.feature.upgrade.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.R
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Order
import com.gotest.app.navigation.PaymentPendingDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val TICK_MS = 1_000L

@HiltViewModel
class PaymentPendingViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val orderId = savedStateHandle.toRoute<PaymentPendingDestination>().orderId
        private val refresh = MutableStateFlow(0)
        private val isConfirming = MutableStateFlow(false)

        private val _effects = Channel<PaymentPendingEffect>(Channel.BUFFERED)
        val effects: Flow<PaymentPendingEffect> = _effects.receiveAsFlow()

        /** Wall-clock time once a second, driving the Jam : Menit : Detik countdown. */
        private val now: Flow<Long> =
            flow {
                while (true) {
                    emit(System.currentTimeMillis())
                    delay(TICK_MS)
                }
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<PaymentPendingUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        subscriptionRepository.observeOrder(orderId),
                        now,
                        isConfirming,
                    ) { order, nowMillis, confirming -> toUiState(order, nowMillis, confirming) }
                        .onStart { emit(PaymentPendingUiState.Loading) }
                        .catch { emit(PaymentPendingUiState.Error) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    PaymentPendingUiState.Loading,
                )

        fun onEvent(event: PaymentPendingUiEvent) {
            when (event) {
                PaymentPendingUiEvent.ConfirmPayment -> confirmPayment()
                PaymentPendingUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private fun toUiState(
            order: Order,
            nowMillis: Long,
            confirming: Boolean,
        ): PaymentPendingUiState {
            val remainingMillis = (order.expiresAtEpochMillis - nowMillis).coerceAtLeast(0L)
            return PaymentPendingUiState.Success(
                order = order,
                remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis),
                isConfirming = confirming,
            )
        }

        private fun confirmPayment() {
            if (isConfirming.value) return
            isConfirming.value = true
            viewModelScope.launch {
                subscriptionRepository
                    .confirmPayment(orderId)
                    .onSuccess { _effects.send(PaymentPendingEffect.PaymentConfirmed(orderId)) }
                    .onFailure {
                        _effects.send(
                            PaymentPendingEffect.ShowMessage(R.string.upgrade_payment_confirm_error),
                        )
                    }
                isConfirming.value = false
            }
        }
    }
