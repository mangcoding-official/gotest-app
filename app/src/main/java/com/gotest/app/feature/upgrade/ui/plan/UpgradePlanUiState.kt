package com.gotest.app.feature.upgrade.ui.plan

import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.PlanTier

sealed interface UpgradePlanUiState {
    data object Loading : UpgradePlanUiState

    data object Error : UpgradePlanUiState

    /** The API returned no plans. */
    data object Empty : UpgradePlanUiState

    data class Success(
        val plans: List<Plan>,
        val selectedPlanId: String,
        val currentTier: PlanTier,
    ) : UpgradePlanUiState {
        val selectedPlan: Plan get() = plans.firstOrNull { it.id == selectedPlanId } ?: plans.first()

        val isPremium: Boolean get() = currentTier == PlanTier.Premium

        /** Only a paid plan the student is not on yet leads to checkout. */
        val canCheckout: Boolean get() = !isPremium && selectedPlan.tier == PlanTier.Premium
    }
}

sealed interface UpgradePlanUiEvent {
    data class SelectPlan(
        val planId: String,
    ) : UpgradePlanUiEvent

    data object Retry : UpgradePlanUiEvent
}
