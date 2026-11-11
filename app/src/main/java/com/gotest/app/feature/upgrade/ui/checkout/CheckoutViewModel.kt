package com.gotest.app.feature.upgrade.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.R
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.feature.upgrade.domain.CheckoutPricing
import com.gotest.app.navigation.CheckoutDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** What the student changes on the screen, as opposed to what the repositories load. */
private data class CheckoutForm(
    val method: PaymentMethodType = PaymentMethodType.VirtualAccount,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
class CheckoutViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        userRepository: UserRepository,
        private val subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val planId = savedStateHandle.toRoute<CheckoutDestination>().planId
        private val refresh = MutableStateFlow(0)
        private val form = MutableStateFlow(CheckoutForm())

        private val _effects = Channel<CheckoutEffect>(Channel.BUFFERED)
        val effects: Flow<CheckoutEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<CheckoutUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        userRepository.observeProfile(),
                        subscriptionRepository.observePlans(),
                        form,
                    ) { profile, plans, formState ->
                        val plan = plans.firstOrNull { it.id == planId }
                        if (plan == null) {
                            CheckoutUiState.Error
                        } else {
                            CheckoutUiState.Success(
                                fullName = profile.fullName,
                                email = profile.email,
                                plan = plan,
                                selectedMethod = formState.method,
                                feeRupiah = plan.adminFeeRupiah,
                                totalRupiah = CheckoutPricing.total(plan),
                                isSubmitting = formState.isSubmitting,
                            )
                        }
                    }.onStart { emit(CheckoutUiState.Loading) }
                        .catch { emit(CheckoutUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CheckoutUiState.Loading)

        fun onEvent(event: CheckoutUiEvent) {
            when (event) {
                is CheckoutUiEvent.SelectMethod -> form.update { it.copy(method = event.method) }
                CheckoutUiEvent.Pay -> pay()
                CheckoutUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private fun pay() {
            val current = form.value
            if (current.isSubmitting) return
            if (current.method != PaymentMethodType.VirtualAccount) {
                // Only the bank transfer flow is designed; QRIS and e-wallet come later.
                _effects.trySend(CheckoutEffect.ShowMessage(R.string.coming_soon))
                return
            }
            form.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                runCatching { subscriptionRepository.createOrder(planId, current.method) }
                    .onSuccess { order -> _effects.send(CheckoutEffect.OpenPayment(order.id)) }
                    .onFailure { _effects.send(CheckoutEffect.ShowMessage(R.string.upgrade_checkout_error)) }
                form.update { it.copy(isSubmitting = false) }
            }
        }
    }
