package com.gotest.app.feature.tryout.ui.catalog

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
import com.gotest.app.feature.tryout.domain.TryoutPackage

/**
 * A Try Out package's start sheet opened outside the catalog: Beranda's "Paket Untukmu" and Cari (T-026 A2c-1), as
 * the web opens TryoutStartModal there. The catalog's own view model does the start, with the catalog's rules.
 */
@Composable
fun TryoutStartElsewhere(
    packageId: Long,
    onClose: () -> Unit,
    onOpenRunner: (attemptId: Long, packageName: String) -> Unit,
    onOpenResult: (attemptId: Long) -> Unit,
    modifier: Modifier = Modifier,
    item: TryoutPackage? = null,
    viewModel: TryoutCatalogViewModel = hiltViewModel(key = START_ELSEWHERE),
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
            is TryoutCatalogEffect.OpenRunner -> onOpenRunner(effect.attemptId, effect.packageName)
            is TryoutCatalogEffect.OpenResult -> onOpenResult(effect.attemptId)
        }
    }
    uiState.sheet?.let { sheet ->
        TryoutStartSheet(
            sheet = sheet,
            modifier = modifier,
            access = tryoutAccess(uiState.access, sheet.item.premiumLocked),
            actions =
                SheetCallbacks(
                    onStart = viewModel::start,
                    onClose = {
                        viewModel.closeSheet()
                        if (viewModel.uiState.value.sheet == null) currentOnClose()
                    },
                ),
        )
    }
}

private const val START_ELSEWHERE = "tryout-start-elsewhere"
