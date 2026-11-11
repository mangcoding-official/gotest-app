package com.gotest.app.feature.exam.ui.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.feature.exam.domain.ExamRules
import com.gotest.app.navigation.ExamDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ONE_SECOND_MS = 1_000L

@HiltViewModel
class ExamViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val examRepository: ExamRepository,
    ) : ViewModel() {
        private val packageId = savedStateHandle.toRoute<ExamDestination>().packageId

        private val _uiState = MutableStateFlow<ExamUiState>(ExamUiState.Loading)
        val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

        private val _effects = Channel<ExamEffect>(Channel.BUFFERED)
        val effects: Flow<ExamEffect> = _effects.receiveAsFlow()

        private var loadJob: Job? = null
        private var timerJob: Job? = null

        init {
            load()
        }

        fun onEvent(event: ExamUiEvent) {
            when (event) {
                is ExamUiEvent.AnswerChanged -> {
                    changeAnswer(event.answer)
                }

                ExamUiEvent.ToggleMark -> {
                    toggleMark()
                }

                ExamUiEvent.Previous -> {
                    moveTo { ExamRules.previousIndex(it.currentIndex) }
                }

                ExamUiEvent.Next -> {
                    moveTo { ExamRules.nextIndex(it.currentIndex, it.questions.size) }
                }

                is ExamUiEvent.JumpTo -> {
                    moveTo { ExamRules.clampIndex(event.index, it.questions.size) }
                }

                ExamUiEvent.OpenNavigator -> {
                    updateSuccess { it.copy(isNavigatorOpen = true) }
                }

                ExamUiEvent.DismissNavigator -> {
                    updateSuccess { it.copy(isNavigatorOpen = false) }
                }

                ExamUiEvent.RequestFinish -> {
                    updateSuccess {
                        it.copy(
                            isNavigatorOpen = false,
                            isFinishConfirmOpen = true,
                        )
                    }
                }

                ExamUiEvent.DismissFinish -> {
                    updateSuccess { it.copy(isFinishConfirmOpen = false) }
                }

                ExamUiEvent.ConfirmFinish -> {
                    submit()
                }

                ExamUiEvent.Retry -> {
                    load()
                }
            }
        }

        private fun load() {
            loadJob?.cancel()
            timerJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    flow { emit(examRepository.startOrResume(packageId)) }
                        .map { ExamUiState.from(it) }
                        .onStart { emit(ExamUiState.Loading) }
                        .catch { emit(ExamUiState.Error) }
                        .collect { state ->
                            _uiState.value = state
                            if (state is ExamUiState.Success) startTimer(state.remainingSeconds)
                        }
                }
        }

        /** Counts the tryout down once a second and submits on zero. Latihan has no timer. */
        private fun startTimer(initialSeconds: Int?) {
            if (initialSeconds == null) return
            timerJob?.cancel()
            timerJob =
                viewModelScope.launch {
                    var remaining: Int = initialSeconds
                    while (remaining > 0) {
                        delay(ONE_SECOND_MS)
                        remaining = ExamRules.tick(remaining)
                        val seconds = remaining
                        updateSuccess { it.copy(remainingSeconds = seconds) }
                    }
                    submit()
                }
        }

        private fun changeAnswer(answer: QuestionAnswer) {
            val state = _uiState.value as? ExamUiState.Success ?: return
            if (state.isSubmitting) return
            val questionId = state.currentQuestion.id
            updateSuccess { it.copy(answers = it.answers + (questionId to answer)) }
            persist { examRepository.saveAnswer(state.attemptId, questionId, answer) }
        }

        private fun toggleMark() {
            val state = _uiState.value as? ExamUiState.Success ?: return
            val questionId = state.currentQuestion.id
            val marked = !state.isCurrentMarked
            updateSuccess {
                it.copy(markedIds = if (marked) it.markedIds + questionId else it.markedIds - questionId)
            }
            persist { examRepository.setMarked(state.attemptId, questionId, marked) }
        }

        private fun moveTo(target: (ExamUiState.Success) -> Int) {
            updateSuccess { it.copy(currentIndex = target(it), isNavigatorOpen = false) }
        }

        private fun submit() {
            val state = _uiState.value as? ExamUiState.Success ?: return
            if (state.isSubmitting) return
            timerJob?.cancel()
            updateSuccess { it.copy(isSubmitting = true, isNavigatorOpen = false, isFinishConfirmOpen = false) }
            viewModelScope.launch {
                flow { emit(examRepository.submit(state.attemptId)) }
                    .catch {
                        updateSuccess { it.copy(isSubmitting = false) }
                        _effects.send(ExamEffect.SubmitFailed)
                    }.collect { attemptId -> _effects.send(ExamEffect.Submitted(attemptId)) }
            }
        }

        /** Saves in the background; a failure only tells the student, the local state stays. */
        private fun persist(block: suspend () -> Unit) {
            viewModelScope.launch {
                flow { emit(block()) }
                    .catch { _effects.send(ExamEffect.SaveFailed) }
                    .collect()
            }
        }

        private inline fun updateSuccess(transform: (ExamUiState.Success) -> ExamUiState.Success) {
            _uiState.update { state -> if (state is ExamUiState.Success) transform(state) else state }
        }
    }
