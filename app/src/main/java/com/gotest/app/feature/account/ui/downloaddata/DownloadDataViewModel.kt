package com.gotest.app.feature.account.ui.downloaddata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.UserProfile
import com.gotest.app.feature.account.domain.AccountDataCategory
import com.gotest.app.feature.account.domain.AccountDataRepository
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

private data class DownloadDataForm(
    val selected: Set<AccountDataCategory> = defaultDataCategories,
    val isSending: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadDataViewModel
    @Inject
    constructor(
        userRepository: UserRepository,
        examRepository: ExamRepository,
        private val accountDataRepository: AccountDataRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val form = MutableStateFlow(DownloadDataForm())

        private val content: Flow<DownloadDataUiState> =
            retryKey.flatMapLatest {
                combine<UserProfile, List<AttemptSummary>, DownloadDataUiState>(
                    userRepository.observeProfile(),
                    examRepository.observeHistory(),
                ) { profile, history ->
                    DownloadDataUiState.Success(email = profile.email, attemptCount = history.size)
                }.onStart { emit(DownloadDataUiState.Loading) }
                    .catch { emit(DownloadDataUiState.Error) }
            }

        val uiState: StateFlow<DownloadDataUiState> =
            combine(content, form) { state, input ->
                if (state is DownloadDataUiState.Success) {
                    state.copy(selected = input.selected, isSending = input.isSending)
                } else {
                    state
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DownloadDataUiState.Loading)

        private val _effects = Channel<DownloadDataEffect>(Channel.BUFFERED)
        val effects: Flow<DownloadDataEffect> = _effects.receiveAsFlow()

        fun onEvent(event: DownloadDataUiEvent) {
            when (event) {
                DownloadDataUiEvent.Retry -> {
                    retryKey.update { it + 1 }
                }

                is DownloadDataUiEvent.Toggle -> {
                    form.update { current ->
                        if (current.isSending) {
                            current
                        } else {
                            current.copy(
                                selected = current.selected.toggle(event.category),
                            )
                        }
                    }
                }

                DownloadDataUiEvent.Submit -> {
                    submit()
                }
            }
        }

        private fun submit() {
            val state = uiState.value as? DownloadDataUiState.Success
            if (state == null || !state.canSubmit) return
            viewModelScope.launch {
                form.update { it.copy(isSending = true) }
                accountDataRepository
                    .requestDataCopy(state.selected)
                    // Stays "sending" on success so the button cannot fire again while leaving.
                    .onSuccess { _effects.send(DownloadDataEffect.Requested) }
                    .onFailure {
                        form.update { it.copy(isSending = false) }
                        _effects.send(DownloadDataEffect.ShowMessage(R.string.account_download_error))
                    }
            }
        }
    }

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
