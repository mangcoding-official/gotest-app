package com.gotest.app.feature.studypackage.ui.tryoutdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.navigation.TryoutDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TryoutDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: StudyPackageRepository,
    ) : ViewModel() {
        private val packageId = savedStateHandle.toRoute<TryoutDetailDestination>().packageId
        private val refreshKey = MutableStateFlow(0)

        private val _effects = Channel<TryoutDetailEffect>(Channel.BUFFERED)
        val effects: Flow<TryoutDetailEffect> = _effects.receiveAsFlow()

        val uiState: StateFlow<TryoutDetailUiState> =
            refreshKey
                .flatMapLatest {
                    repository
                        .observePackageDetail(packageId)
                        .map<StudyPackageDetail, TryoutDetailUiState> { TryoutDetailUiState.Success(it) }
                        .onStart { emit(TryoutDetailUiState.Loading) }
                        .catch { emit(TryoutDetailUiState.Error) }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = TryoutDetailUiState.Loading,
                )

        fun onEvent(event: TryoutDetailUiEvent) {
            when (event) {
                TryoutDetailUiEvent.Retry -> {
                    refreshKey.update { it + 1 }
                }

                TryoutDetailUiEvent.BookmarkClicked -> {
                    viewModelScope.launch {
                        _effects.send(TryoutDetailEffect.ShowComingSoon)
                    }
                }
            }
        }
    }
