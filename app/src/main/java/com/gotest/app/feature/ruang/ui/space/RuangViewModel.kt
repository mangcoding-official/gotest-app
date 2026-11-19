package com.gotest.app.feature.ruang.ui.space

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.navigation.RuangDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class RuangUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: RuangPage? = null,
    /** The Ujian card: the plan includes Ujian, or there is a schedule or result here (API-K7 B). */
    val hasExams: Boolean = false,
)

/** A student's Mitra space (web: Ruang/Show). Try Out, Latihan, and tasks join with A2c and Rencana Belajar. */
@HiltViewModel
class RuangViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: RuangRepository,
    ) : ViewModel() {
        val code: String = savedStateHandle.toRoute<RuangDestination>().code

        private val _uiState = MutableStateFlow(RuangUiState())
        val uiState: StateFlow<RuangUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                val members = async { repository.memberships() }
                repository
                    .ruang(code)
                    .onSuccess { page ->
                        val hasExams =
                            members
                                .await()
                                .getOrNull()
                                ?.firstOrNull { it.code == code }
                                ?.hasExams ?: false
                        _uiState.update { RuangUiState(isLoading = false, page = page, hasExams = hasExams) }
                    }.onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }
    }
