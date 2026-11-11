package com.gotest.app.feature.account.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.Subscription
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.SettingSwitch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data object Error : SettingsUiState

    @Immutable
    data class Success(
        val settings: AccountSettings,
        val subscription: Subscription,
        /** False once [Subscription.activeUntil] has passed ("Berakhir" chip). */
        val isSubscriptionActive: Boolean,
    ) : SettingsUiState
}

sealed interface SettingsUiEvent {
    data object Retry : SettingsUiEvent

    data class SwitchChange(
        val switch: SettingSwitch,
        val enabled: Boolean,
    ) : SettingsUiEvent
}

sealed interface SettingsEffect {
    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : SettingsEffect
}
