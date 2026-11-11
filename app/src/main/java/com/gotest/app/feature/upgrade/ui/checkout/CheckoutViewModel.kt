package com.gotest.app.feature.upgrade.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.R
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.OrderPromo
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.feature.upgrade.domain.CheckoutPricing
import com.gotest.app.feature.upgrade.domain.Promo
import com.gotest.app.feature.upgrade.domain.PromoRepository
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
import kotlinx.coroutines.flow.flowOf
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
    val wallet: EWallet = EWallet.GoPay,
    val isWalletSheetVisible: Boolean = false,
    val isSubmitting: Boolean = false,
)

@HiltViewModel
class CheckoutViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        userRepository: UserRepository,
        promoRepository: PromoRepository,
        private val subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<CheckoutDestination>()
        private val planId = route.planId
        private val refresh = MutableStateFlow(0)
        private val form = MutableStateFlow(CheckoutForm())

        /** The promo from the promo screen; an ended promo just drops back to the plan price. */
        private val promo: Flow<Promo?> = route.promoId?.let(promoRepository::observePromo) ?: flowOf(null)

        private val _effects = Channel<CheckoutEffect>(Channel.BUFFERED)
        val effects: Flow<CheckoutEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<CheckoutUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        userRepository.observeProfile(),
                        subscriptionRepository.observePlans(),
                        promo,
                        form,
                    ) { profile, plans, loadedPromo, formState ->
                        val plan = plans.firstOrNull { it.id == planId }
                        if (plan == null) {
                            CheckoutUiState.Error
                        } else {
                            val appliedPromo = loadedPromo?.takeIf { it.planId == plan.id }
                            CheckoutUiState.Success(
                                fullName = profile.fullName,
                                email = profile.email,
                                plan = plan,
                                promo = appliedPromo,
                                selectedMethod = formState.method,
                                selectedWallet = formState.wallet,
                                isWalletSheetVisible = formState.isWalletSheetVisible,
                                priceRupiah = CheckoutPricing.price(plan, appliedPromo),
                                feeRupiah = plan.adminFeeRupiah,
                                totalRupiah = CheckoutPricing.total(plan, appliedPromo),
                                isSubmitting = formState.isSubmitting,
                            )
                        }
                    }.onStart { emit(CheckoutUiState.Loading) }
                        .catch { emit(CheckoutUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CheckoutUiState.Loading)

        fun onEvent(event: CheckoutUiEvent) {
            when (event) {
                is CheckoutUiEvent.SelectMethod -> {
                    val isWallet = event.method == PaymentMethodType.EWallet
                    form.update { it.copy(method = event.method, isWalletSheetVisible = isWallet) }
                }

                is CheckoutUiEvent.SelectWallet -> {
                    form.update { it.copy(wallet = event.wallet) }
                }

                CheckoutUiEvent.DismissWalletSheet -> {
                    form.update { it.copy(isWalletSheetVisible = false) }
                }

                CheckoutUiEvent.Pay -> {
                    if (form.value.method == PaymentMethodType.EWallet) {
                        form.update { it.copy(isWalletSheetVisible = true) }
                    } else {
                        pay()
                    }
                }

                CheckoutUiEvent.PayWithWallet -> {
                    pay()
                }

                CheckoutUiEvent.Retry -> {
                    refresh.update { it + 1 }
                }
            }
        }

        private fun pay() {
            val current = form.value
            val state = uiState.value as? CheckoutUiState.Success ?: return
            if (current.isSubmitting) return
            val orderPromo = state.promo?.let { OrderPromo(promoId = it.id, priceRupiah = state.priceRupiah) }
            form.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                runCatching {
                    subscriptionRepository.createOrder(
                        planId = planId,
                        method = current.method,
                        eWallet = current.wallet.takeIf { current.method == PaymentMethodType.EWallet },
                        promo = orderPromo,
                    )
                }.onSuccess { order -> _effects.send(CheckoutEffect.OpenPayment(order.id)) }
                    .onFailure { _effects.send(CheckoutEffect.ShowMessage(R.string.upgrade_checkout_error)) }
                // Closing the sheet also keeps an error snackbar from hiding behind it.
                form.update { it.copy(isSubmitting = false, isWalletSheetVisible = false) }
            }
        }
    }
