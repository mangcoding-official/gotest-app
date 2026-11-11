package com.gotest.app.feature.multiplayer.ui.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.multiplayer.domain.GameCategory
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.navigation.CreateGameDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Number of topics preselected for a fresh form, as in the design. */
private const val DEFAULT_TOPIC_COUNT = 2

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreateGameViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: MultiplayerRepository,
    ) : ViewModel() {
        /** "Buat Ulang dengan Pengaturan Sama": the game whose settings fill the form. */
        private val copyFromGameCode = savedStateHandle.toRoute<CreateGameDestination>().copyFromGameCode
        private val refresh = MutableStateFlow(0)
        private val form = MutableStateFlow<CreateGameForm?>(null)
        private val submitting = MutableStateFlow(false)

        private val catalog: Flow<CatalogLoad> =
            refresh
                .flatMapLatest {
                    combine<List<GameCategory>, GameSettings?, CatalogLoad>(
                        repository.observeCatalog(),
                        copiedSettings(),
                        CatalogLoad::Loaded,
                    ).onStart { emit(CatalogLoad.Loading) }
                        .catch { emit(CatalogLoad.Failed) }
                }.onEach { load ->
                    if (load is CatalogLoad.Loaded && form.value == null && load.categories.isNotEmpty()) {
                        form.value = initialForm(load.categories, load.copied)
                    }
                }

        val uiState: StateFlow<CreateGameUiState> =
            combine(catalog, form, submitting) { load, current, isSubmitting ->
                when {
                    load is CatalogLoad.Failed -> CreateGameUiState.Error
                    current == null -> CreateGameUiState.Loading
                    else -> CreateGameUiState.Success(current, isSubmitting)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CreateGameUiState.Loading)

        private val _effects = Channel<CreateGameEffect>(Channel.BUFFERED)
        val effects: Flow<CreateGameEffect> = _effects.receiveAsFlow()

        fun onEvent(event: CreateGameUiEvent) {
            when (event) {
                CreateGameUiEvent.Retry -> refresh.update { it + 1 }
                CreateGameUiEvent.Submit -> submit()
                else -> updateForm { it.reduce(event) }
            }
        }

        /** Settings of the game to copy, or null for a fresh form. */
        private fun copiedSettings(): Flow<GameSettings?> =
            flow { emit(copyFromGameCode?.let { repository.observeGame(it).first().settings }) }

        private fun updateForm(transform: (CreateGameForm) -> CreateGameForm) {
            form.update { current -> current?.let(transform) }
        }

        private fun submit() {
            val current = form.value
            if (current == null || !current.canSubmit || submitting.value) return
            viewModelScope.launch {
                submitting.value = true
                repository
                    .createGame(current.toSettings())
                    .onSuccess { _effects.send(CreateGameEffect.Created(it)) }
                    .onFailure { _effects.send(CreateGameEffect.Failed) }
                submitting.value = false
            }
        }

        private sealed interface CatalogLoad {
            data object Loading : CatalogLoad

            data object Failed : CatalogLoad

            data class Loaded(
                val categories: List<GameCategory>,
                val copied: GameSettings?,
            ) : CatalogLoad
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** A copy of [copied], or the design's defaults with the first topics preselected. */
private fun initialForm(
    categories: List<GameCategory>,
    copied: GameSettings?,
): CreateGameForm {
    val form = CreateGameForm(categories = categories)
    if (copied != null) return form.withSettings(copied)
    return form.copy(
        selectedTopics =
            form.subtest.topics
                .take(DEFAULT_TOPIC_COUNT)
                .map { it.name },
    )
}

/** Applies a form edit; Retry and Submit are handled by the ViewModel. */
private fun CreateGameForm.reduce(event: CreateGameUiEvent): CreateGameForm =
    when (event) {
        is CreateGameUiEvent.TypeChange -> {
            copy(type = event.type)
        }

        is CreateGameUiEvent.CategoryChange -> {
            val category = categories.firstOrNull { it.name == event.name } ?: return this
            copy(
                categoryName = category.name,
                subtestName = category.subtests.first().name,
                selectedTopics = emptyList(),
            )
        }

        is CreateGameUiEvent.SubtestChange -> {
            copy(subtestName = event.name, selectedTopics = emptyList())
        }

        is CreateGameUiEvent.TopicAdd -> {
            copy(selectedTopics = selectedTopics + event.name)
        }

        is CreateGameUiEvent.TopicRemove -> {
            copy(selectedTopics = selectedTopics - event.name)
        }

        CreateGameUiEvent.DecreaseQuestions -> {
            withQuestionCount(questionCount - 1)
        }

        CreateGameUiEvent.IncreaseQuestions -> {
            withQuestionCount(questionCount + 1)
        }

        is CreateGameUiEvent.DurationChange -> {
            withDuration(event.minutes)
        }

        CreateGameUiEvent.Retry, CreateGameUiEvent.Submit -> {
            this
        }
    }
