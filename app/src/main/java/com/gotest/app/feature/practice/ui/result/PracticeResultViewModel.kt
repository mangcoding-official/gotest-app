package com.gotest.app.feature.practice.ui.result

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.PracticeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PracticeResultUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val result: PracticeResult? = null,
    /** An empty question answered here to open its pembahasan; it never changes the result (ReviewCard). */
    val tries: Map<Long, AnswerValue> = emptyMap(),
    val revealed: Set<Long> = emptySet(),
)

/** The pembahasan of a finished Latihan (web Practice/Result with PembahasanPage, `requireResponseForEmpty`). */
@HiltViewModel
class PracticeResultViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: PracticeRepository,
    ) : ViewModel() {
        val attemptId: Long = checkNotNull(savedStateHandle[ARG_ATTEMPT])

        private val _uiState = MutableStateFlow(PracticeResultUiState())
        val uiState: StateFlow<PracticeResultUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                repository
                    .result(attemptId)
                    .onSuccess { result -> _uiState.update { it.copy(isLoading = false, result = result) } }
                    .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }

        fun onTry(
            itemId: Long,
            value: AnswerValue,
        ) = _uiState.update { it.copy(tries = it.tries + (itemId to value)) }

        fun reveal(itemId: Long) = _uiState.update { it.copy(revealed = it.revealed + itemId) }

        companion object {
            const val ARG_ATTEMPT = "attemptId"
        }
    }
