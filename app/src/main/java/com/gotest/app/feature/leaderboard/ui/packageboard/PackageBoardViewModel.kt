package com.gotest.app.feature.leaderboard.ui.packageboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.PackageBoardEntry
import com.gotest.app.feature.leaderboard.domain.PackageBoardPage
import com.gotest.app.feature.leaderboard.ui.components.BoardRow
import com.gotest.app.navigation.PackageLeaderboardDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PackageBoardUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val page: PackageBoardPage? = null,
)

/** One package's board (web Leaderboard/Package): Try Out from Riwayat or "Per Paket", Latihan from "Latihan". */
@HiltViewModel
class PackageBoardViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: LeaderboardPagesRepository,
    ) : ViewModel() {
        private val destination = savedStateHandle.toRoute<PackageLeaderboardDestination>()

        private val _uiState = MutableStateFlow(PackageBoardUiState())
        val uiState: StateFlow<PackageBoardUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            val id = destination.packageId.toLongOrNull()
            if (id == null) {
                _uiState.update { PackageBoardUiState(isLoading = false, loadFailed = true) }
                return
            }
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            viewModelScope.launch {
                val result = if (destination.practice) repository.practiceBoard(id) else repository.tryoutBoard(id)
                result
                    .onSuccess { page -> _uiState.update { PackageBoardUiState(isLoading = false, page = page) } }
                    .onFailure { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
            }
        }
    }

internal fun PackageBoardEntry.toRow(): BoardRow =
    BoardRow(
        rank = rank,
        userId = userId,
        name = name,
        points = points,
        change = change,
        isCurrentUser = isCurrentUser,
        durationUsed = durationUsed,
    )
