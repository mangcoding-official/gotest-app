package com.gotest.app.feature.ruang.ui.result

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.navigation.UjianResultDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ExamResultUiState(
    val isLoading: Boolean = true,
    val page: ExamResultPage? = null,
    /** The server's refusal (for example 409 "Ujian ini belum dikumpulkan."), or null for no connection. */
    val errorMessage: String? = null,
)

/** The result of one Ujian attempt (web: Ruang/Exams/Result). */
@HiltViewModel
class ExamResultViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val exams: ExamRepository,
        private val local: ExamLocalStore,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<UjianResultDestination>()
        val code: String = route.code

        private val _uiState = MutableStateFlow(ExamResultUiState())
        val uiState: StateFlow<ExamResultUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            _uiState.update { ExamResultUiState() }
            viewModelScope.launch {
                exams
                    .result(code, route.attemptId)
                    .onSuccess { page ->
                        // A result means the attempt is over, also when it ended away from the work screen
                        // (time up while the app was closed): nothing of it stays on the phone.
                        local.forget(route.attemptId)
                        _uiState.update { ExamResultUiState(isLoading = false, page = page) }
                    }.onFailure { error ->
                        _uiState.update {
                            ExamResultUiState(
                                isLoading = false,
                                errorMessage = (error as? ApiError)?.message,
                            )
                        }
                    }
            }
        }
    }
