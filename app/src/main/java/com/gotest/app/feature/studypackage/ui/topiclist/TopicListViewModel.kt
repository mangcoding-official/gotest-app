package com.gotest.app.feature.studypackage.ui.topiclist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.Subject
import com.gotest.app.core.model.Topic
import com.gotest.app.feature.studypackage.ui.components.PracticeSheetRequest
import com.gotest.app.feature.studypackage.ui.components.observePracticeSheet
import com.gotest.app.navigation.TopicListDestination
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

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TopicListViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: StudyPackageRepository,
    ) : ViewModel() {
        private val subjectId = savedStateHandle.toRoute<TopicListDestination>().subjectId
        private val refreshKey = MutableStateFlow(0)
        private val sheetRequest = MutableStateFlow<PracticeSheetRequest?>(null)

        private val _effects = Channel<TopicListEffect>(Channel.BUFFERED)
        val effects: Flow<TopicListEffect> = _effects.receiveAsFlow()

        private val content: Flow<TopicListContent> =
            refreshKey.flatMapLatest {
                repository
                    .observeSubject(subjectId)
                    .flatMapLatest { subject ->
                        combine(
                            repository.observeCategory(subject.categoryId),
                            repository.observeTopics(subjectId),
                        ) { category, topics -> contentOf(subject, category, topics) }
                    }.onStart { emit(TopicListContent.Loading) }
                    .catch { emit(TopicListContent.Error) }
            }

        val uiState: StateFlow<TopicListUiState> =
            combine(content, repository.observePracticeSheet(sheetRequest), ::TopicListUiState)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TopicListUiState())

        fun onEvent(event: TopicListUiEvent) {
            when (event) {
                TopicListUiEvent.Retry -> {
                    refreshKey.update { it + 1 }
                }

                is TopicListUiEvent.TopicActionClicked -> {
                    sheetRequest.value = PracticeSheetRequest(event.packageId)
                }

                TopicListUiEvent.SheetDismissed -> {
                    sheetRequest.value = null
                }

                TopicListUiEvent.SheetRetry -> {
                    sheetRequest.update { it?.copy(attempt = it.attempt + 1) }
                }

                is TopicListUiEvent.SheetStartClicked -> {
                    sheetRequest.value = null
                    viewModelScope.launch { _effects.send(TopicListEffect.StartExam(event.packageId)) }
                }
            }
        }

        private fun contentOf(
            subject: Subject,
            category: StudyCategory,
            topics: List<Topic>,
        ): TopicListContent {
            // "SLTA / Sederajat" -> "SLTA", as in the design's subtitle.
            val levelLabel = category.name.substringBefore("/").trim()
            return if (topics.isEmpty()) {
                TopicListContent.Empty(subject, levelLabel)
            } else {
                TopicListContent.Success(subject, levelLabel, topics)
            }
        }
    }
