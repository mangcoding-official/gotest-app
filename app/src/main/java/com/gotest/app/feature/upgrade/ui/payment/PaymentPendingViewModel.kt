package com.gotest.app.feature.upgrade.ui.payment

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.R
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.OrderStatus
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
import kotlinx.coroutines.flow.first
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

/** Survives process death, so the wallet app is not opened a second time. */
private const val KEY_WALLET_OPENED = "wallet_opened"

@HiltViewModel
class PaymentPendingViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val subscriptionRepository: SubscriptionRepository,
        private val qrisImageExporter: QrisImageExporter,
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

        init {
            openWalletOnce()
        }

        fun onEvent(event: PaymentPendingUiEvent) {
            when (event) {
                PaymentPendingUiEvent.ConfirmPayment -> confirmPayment()
                is PaymentPendingUiEvent.SaveQr -> saveQr(event.destination)
                PaymentPendingUiEvent.ShareQr -> shareQr()
                PaymentPendingUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        /**
         * "Lanjutkan di aplikasi …" says the wallet app was already opened, so an e-wallet order
         * opens it once when the screen first appears; "Buka … Lagi" does it again on request.
         */
        private fun openWalletOnce() {
            if (savedStateHandle.get<Boolean>(KEY_WALLET_OPENED) == true) return
            viewModelScope.launch {
                val order = runCatching { subscriptionRepository.observeOrder(orderId).first() }.getOrNull()
                val wallet = order?.eWallet ?: return@launch
                val isPayable =
                    order.status == OrderStatus.Pending && order.expiresAtEpochMillis > System.currentTimeMillis()
                if (isPayable) {
                    savedStateHandle[KEY_WALLET_OPENED] = true
                    _effects.send(PaymentPendingEffect.OpenWalletApp(wallet))
                }
            }
        }

        private fun qrisPayload(): String? =
            (uiState.value as? PaymentPendingUiState.Success)
                ?.order
                ?.qris
                ?.payload

        private fun saveQr(destination: Uri) {
            val payload = qrisPayload() ?: return
            viewModelScope.launch {
                val message =
                    if (runCatching { qrisImageExporter.save(payload, destination) }.isSuccess) {
                        R.string.upgrade_qris_saved
                    } else {
                        R.string.upgrade_qris_save_error
                    }
                _effects.send(PaymentPendingEffect.ShowMessage(message))
            }
        }

        private fun shareQr() {
            val payload = qrisPayload() ?: return
            viewModelScope.launch {
                runCatching { qrisImageExporter.shareableUri(payload) }
                    .onSuccess { _effects.send(PaymentPendingEffect.ShareQrImage(it)) }
                    .onFailure { _effects.send(PaymentPendingEffect.ShowMessage(R.string.upgrade_qris_share_error)) }
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
