package com.gotest.app.feature.exam.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.navigation.DiscussionSummaryDestination
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
class DiscussionSummaryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        examRepository: ExamRepository,
    ) : ViewModel() {
        private val attemptId = savedStateHandle.toRoute<DiscussionSummaryDestination>().attemptId
        private val refresh = MutableStateFlow(0)
        private val selectedTopic = MutableStateFlow<String?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<DiscussionSummaryUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        examRepository.observeReview(attemptId),
                        examRepository.observeResult(attemptId),
                        selectedTopic,
                    ) { reviews, result, topic ->
                        DiscussionSummaryUiState.from(result.title, result.score, reviews, topic)
                    }.onStart { emit(DiscussionSummaryUiState.Loading) }
                        .catch { emit(DiscussionSummaryUiState.Error) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    DiscussionSummaryUiState.Loading,
                )

        fun onEvent(event: DiscussionSummaryUiEvent) {
            when (event) {
                is DiscussionSummaryUiEvent.SelectTopic -> selectedTopic.value = event.topic
                DiscussionSummaryUiEvent.Retry -> refresh.update { it + 1 }
            }
        }
    }
