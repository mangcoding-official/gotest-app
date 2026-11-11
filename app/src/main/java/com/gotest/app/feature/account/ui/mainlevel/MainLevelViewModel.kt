package com.gotest.app.feature.account.ui.mainlevel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.MainLevel
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/** The student's pick; null keeps the stored level selected. */
private data class MainLevelForm(
    val selectedLevel: MainLevel? = null,
    val isSaving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainLevelViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val form = MutableStateFlow(MainLevelForm())

        private val content: Flow<MainLevelUiState> =
            retryKey.flatMapLatest {
                settingsRepository
                    .observeSettings()
                    .map<AccountSettings, MainLevelUiState> { MainLevelUiState.Success(savedLevel = it.mainLevel) }
                    .onStart { emit(MainLevelUiState.Loading) }
                    .catch { emit(MainLevelUiState.Error) }
            }

        val uiState: StateFlow<MainLevelUiState> =
            combine(content, form) { state, input ->
                if (state is MainLevelUiState.Success) {
                    state.copy(selectedLevel = input.selectedLevel ?: state.savedLevel, isSaving = input.isSaving)
                } else {
                    state
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainLevelUiState.Loading)

        private val _effects = Channel<MainLevelEffect>(Channel.BUFFERED)
        val effects: Flow<MainLevelEffect> = _effects.receiveAsFlow()

        fun onEvent(event: MainLevelUiEvent) {
            when (event) {
                MainLevelUiEvent.Retry -> retryKey.update { it + 1 }
                is MainLevelUiEvent.Select -> form.update { it.copy(selectedLevel = event.level) }
                MainLevelUiEvent.Save -> save()
            }
        }

        private fun save() {
            val state = uiState.value as? MainLevelUiState.Success
            if (state == null || !state.canSave) return
            viewModelScope.launch {
                form.update { it.copy(isSaving = true) }
                settingsRepository
                    .setMainLevel(state.selectedLevel)
                    // Stays "saving" on success so the button cannot fire again while leaving.
                    .onSuccess { _effects.send(MainLevelEffect.Saved) }
                    .onFailure {
                        form.update { it.copy(isSaving = false) }
                        _effects.send(MainLevelEffect.ShowMessage(R.string.account_error_save))
                    }
            }
        }
    }
