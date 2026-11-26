package com.gotest.app.feature.practice.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotest.app.core.ui.ObserveAsEvents
import com.gotest.app.feature.practice.domain.PracticePackage

/**
 * A Latihan package's start sheet opened outside the catalog: Beranda's "Paket Untukmu" and Cari (T-026 A2c-1), as
 * the web opens PracticeStartModal there. The catalog's own view model does the start, so the rules (timer
 * choice, quota, Premium, Lanjutkan, Lihat Pembahasan) are the catalog's.
 */
@Composable
fun PracticeStartElsewhere(
    packageId: Long,
    onClose: () -> Unit,
    onOpenRunner: (attemptId: Long, packageName: String) -> Unit,
    onOpenResult: (attemptId: Long, packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    item: PracticePackage? = null,
    viewModel: PracticeCatalogViewModel = hiltViewModel(key = START_ELSEWHERE),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    // Beranda has the package's row; Cari has only its id, found in the catalog once it is read.
    var opened by remember(packageId) { mutableStateOf(false) }
    LaunchedEffect(packageId, item, uiState.page, uiState.loadFailed) {
        if (opened) return@LaunchedEffect
        val found = item ?: uiState.page?.packages?.firstOrNull { it.id == packageId }
        when {
            found != null -> {
                opened = true
                viewModel.openSheet(found)
            }

            // Gone from the catalog meanwhile, or the catalog could not be read: nothing opens.
            uiState.page != null || uiState.loadFailed -> {
                currentOnClose()
            }
        }
    }
    ObserveAsEvents(viewModel.effects) { effect ->
        currentOnClose()
        when (effect) {
            is PracticeCatalogEffect.OpenRunner -> onOpenRunner(effect.attemptId, effect.packageName)
            is PracticeCatalogEffect.OpenResult -> onOpenResult(effect.attemptId, effect.packageName)
        }
    }
    uiState.sheet?.let { sheet ->
        PracticeStartSheet(
            sheet = sheet,
            modifier = modifier,
            access = startAccess(uiState, sheet.item),
            actions =
                SheetActions(
                    onUseTimer = viewModel::onUseTimer,
                    onStart = viewModel::start,
                    onClose = {
                        viewModel.closeSheet()
                        if (viewModel.uiState.value.sheet == null) currentOnClose()
                    },
                ),
        )
    }
}

private const val START_ELSEWHERE = "practice-start-elsewhere"
