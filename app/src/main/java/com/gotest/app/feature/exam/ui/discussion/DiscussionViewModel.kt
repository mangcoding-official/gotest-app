package com.gotest.app.feature.exam.ui.discussion

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.feature.exam.domain.ReviewRules
import com.gotest.app.navigation.DiscussionDestination
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
private const val KEY_CURRENT_NUMBER = "discussion_current_number"

@HiltViewModel
class DiscussionViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        examRepository: ExamRepository,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<DiscussionDestination>()
        private val refresh = MutableStateFlow(0)

        /** Survives process death so Pembahasan reopens on the same question. */
        private val currentNumber: StateFlow<Int> =
            savedStateHandle.getStateFlow(KEY_CURRENT_NUMBER, route.questionNumber)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<DiscussionUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        examRepository.observeReview(route.attemptId),
                        examRepository.observeResult(route.attemptId),
                        currentNumber,
                    ) { reviews, result, number ->
                        DiscussionUiState.from(result.title, reviews, number)
                    }.onStart { emit(DiscussionUiState.Loading) }
                        .catch { emit(DiscussionUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiscussionUiState.Loading)

        fun onEvent(event: DiscussionUiEvent) {
            when (event) {
                is DiscussionUiEvent.SelectQuestion -> select(event.number)
                DiscussionUiEvent.Previous -> select(currentPosition() - 1)
                DiscussionUiEvent.Next -> select(currentPosition() + 1)
                DiscussionUiEvent.Retry -> refresh.update { it + 1 }
            }
        }

        private fun currentPosition(): Int =
            (uiState.value as? DiscussionUiState.Success)?.currentNumber ?: currentNumber.value

        private fun select(number: Int) {
            val total = (uiState.value as? DiscussionUiState.Success)?.strip?.size ?: return
            savedStateHandle[KEY_CURRENT_NUMBER] = ReviewRules.clampNumber(number, total)
        }
    }
