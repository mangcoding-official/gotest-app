package com.gotest.app.feature.support.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.support.domain.SupportRepository
import com.gotest.app.feature.support.domain.SupportTopic
import com.gotest.app.navigation.HelpTopicDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SupportTopicUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val topic: SupportTopic? = null,
)

/** A help topic (web Support/Topic): `GET /bantuan/{slug}`. */
@HiltViewModel
class SupportTopicViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: SupportRepository,
    ) : ViewModel() {
        private val slug = savedStateHandle.toRoute<HelpTopicDestination>().topicId

        private val _uiState = MutableStateFlow(SupportTopicUiState())
        val uiState: StateFlow<SupportTopicUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                repository
                    .topic(slug)
                    .onSuccess { topic -> _uiState.update { SupportTopicUiState(isLoading = false, topic = topic) } }
                    .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }
    }
