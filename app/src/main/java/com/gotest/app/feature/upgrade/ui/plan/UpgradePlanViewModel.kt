package com.gotest.app.feature.upgrade.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.PlanTier
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
class UpgradePlanViewModel
    @Inject
    constructor(
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val refresh = MutableStateFlow(0)
        private val selectedPlanId = MutableStateFlow<String?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<UpgradePlanUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        subscriptionRepository.observePlans(),
                        subscriptionRepository.observeSubscription(),
                        selectedPlanId,
                    ) { plans, subscription, selectedId -> toUiState(plans, subscription.tier, selectedId) }
                        .onStart { emit(UpgradePlanUiState.Loading) }
                        .catch { emit(UpgradePlanUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UpgradePlanUiState.Loading)

        fun onEvent(event: UpgradePlanUiEvent) {
            when (event) {
                is UpgradePlanUiEvent.SelectPlan -> selectedPlanId.value = event.planId
                UpgradePlanUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private fun toUiState(
            plans: List<Plan>,
            currentTier: PlanTier,
            selectedId: String?,
        ): UpgradePlanUiState {
            if (plans.isEmpty()) return UpgradePlanUiState.Empty
            // The yearly Premium plan is preselected, as in the design.
            val defaultId = (plans.firstOrNull { it.tier == PlanTier.Premium } ?: plans.first()).id
            return UpgradePlanUiState.Success(
                plans = plans,
                selectedPlanId = selectedId?.takeIf { id -> plans.any { it.id == id } } ?: defaultId,
                currentTier = currentTier,
            )
        }
    }
