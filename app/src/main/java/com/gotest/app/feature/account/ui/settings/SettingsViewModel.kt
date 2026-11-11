package com.gotest.app.feature.account.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.Subscription
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.SettingsRepository
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
import java.time.LocalDate
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        subscriptionRepository: SubscriptionRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)

        val uiState: StateFlow<SettingsUiState> =
            retryKey
                .flatMapLatest {
                    combine<AccountSettings, Subscription, SettingsUiState>(
                        settingsRepository.observeSettings(),
                        subscriptionRepository.observeSubscription(),
                    ) { settings, subscription ->
                        SettingsUiState.Success(
                            settings = settings,
                            subscription = subscription,
                            isSubscriptionActive = !subscription.activeUntil.isBefore(LocalDate.now()),
                        )
                    }.onStart { emit(SettingsUiState.Loading) }
                        .catch { emit(SettingsUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState.Loading)

        private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
        val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

        fun onEvent(event: SettingsUiEvent) {
            when (event) {
                SettingsUiEvent.Retry -> {
                    retryKey.update { it + 1 }
                }

                is SettingsUiEvent.SwitchChange -> {
                    viewModelScope.launch {
                        settingsRepository
                            .setSwitch(event.switch, event.enabled)
                            .onFailure { _effects.send(SettingsEffect.ShowMessage(R.string.account_error_save)) }
                    }
                }
            }
        }
    }
