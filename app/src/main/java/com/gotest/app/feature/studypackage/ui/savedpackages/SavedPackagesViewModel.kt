package com.gotest.app.feature.studypackage.ui.savedpackages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.studypackage.domain.SavedPackage
import com.gotest.app.feature.studypackage.domain.SavedPackageRepository
import com.gotest.app.feature.studypackage.ui.components.PracticeSheetRequest
import com.gotest.app.feature.studypackage.ui.components.observePracticeSheet
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Paket Tersimpan. Bookmarks removed here or on a detail screen leave the list at once. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavedPackagesViewModel
    @Inject
    constructor(
        private val repository: StudyPackageRepository,
        private val savedPackageRepository: SavedPackageRepository,
    ) : ViewModel() {
        private val selectedType = MutableStateFlow<PackageType?>(null)
        private val refreshKey = MutableStateFlow(0)
        private val sheetRequest = MutableStateFlow<PracticeSheetRequest?>(null)

        private val _effects = Channel<SavedPackagesEffect>(Channel.BUFFERED)
        val effects: Flow<SavedPackagesEffect> = _effects.receiveAsFlow()

        // Unfiltered; Success may hold an empty list here, the filter below decides Empty.
        private val allSaved: Flow<SavedPackagesContent> =
            refreshKey.flatMapLatest {
                savedPackageRepository
                    .observeSavedPackages()
                    .map<List<SavedPackage>, SavedPackagesContent> { SavedPackagesContent.Success(it) }
                    .onStart { emit(SavedPackagesContent.Loading) }
                    .catch { emit(SavedPackagesContent.Error) }
            }

        val uiState: StateFlow<SavedPackagesUiState> =
            combine(
                selectedType,
                allSaved,
                repository.observePracticeSheet(sheetRequest),
            ) { type, all, sheet ->
                SavedPackagesUiState(
                    selectedType = type,
                    totalCount = (all as? SavedPackagesContent.Success)?.packages?.size,
                    content = all.filteredBy(type),
                    sheet = sheet,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SavedPackagesUiState())

        fun onEvent(event: SavedPackagesUiEvent) {
            when (event) {
                is SavedPackagesUiEvent.TypeSelected -> selectedType.value = event.type
                SavedPackagesUiEvent.Retry -> refreshKey.update { it + 1 }
                is SavedPackagesUiEvent.UnsaveClicked -> unsave(event.packageId)
                is SavedPackagesUiEvent.StartClicked -> start(event.pkg)
                SavedPackagesUiEvent.SheetDismissed -> sheetRequest.value = null
                SavedPackagesUiEvent.SheetRetry -> sheetRequest.update { it?.copy(attempt = it.attempt + 1) }
                is SavedPackagesUiEvent.SheetStartClicked -> startExam(event.packageId)
            }
        }

        private fun unsave(packageId: String) {
            viewModelScope.launch {
                savedPackageRepository.remove(packageId)
                _effects.send(SavedPackagesEffect.PackageRemoved)
            }
        }

        private fun start(pkg: StudyPackage) {
            when (pkg.type) {
                PackageType.Practice -> {
                    sheetRequest.value = PracticeSheetRequest(pkg.id)
                }

                PackageType.Tryout -> {
                    viewModelScope.launch { _effects.send(SavedPackagesEffect.OpenTryout(pkg.id)) }
                }
            }
        }

        private fun startExam(packageId: String) {
            sheetRequest.value = null
            viewModelScope.launch { _effects.send(SavedPackagesEffect.StartExam(packageId)) }
        }
    }

private fun SavedPackagesContent.filteredBy(type: PackageType?): SavedPackagesContent {
    if (this !is SavedPackagesContent.Success) return this
    val visible = if (type == null) packages else packages.filter { it.studyPackage.type == type }
    return if (visible.isEmpty()) SavedPackagesContent.Empty else SavedPackagesContent.Success(visible)
}
