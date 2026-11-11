package com.gotest.app.feature.account.ui.mainlevel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.core.designsystem.icon.GotestIcons
import com.gotest.app.feature.account.domain.MainLevel

sealed interface MainLevelUiState {
    data object Loading : MainLevelUiState

    data object Error : MainLevelUiState

    @Immutable
    data class Success(
        /** The stored level, shown on Pengaturan. */
        val savedLevel: MainLevel,
        val selectedLevel: MainLevel = savedLevel,
        val isSaving: Boolean = false,
    ) : MainLevelUiState {
        /** "Simpan" only does something once another level is picked. */
        val canSave: Boolean get() = selectedLevel != savedLevel && !isSaving
    }
}

sealed interface MainLevelUiEvent {
    data object Retry : MainLevelUiEvent

    data class Select(
        val level: MainLevel,
    ) : MainLevelUiEvent

    data object Save : MainLevelUiEvent
}

sealed interface MainLevelEffect {
    data object Saved : MainLevelEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : MainLevelEffect
}

/** Option title (also the Pengaturan row's subtitle), from frame 23023. */
@get:StringRes
internal val MainLevel.titleRes: Int
    get() =
        when (this) {
            MainLevel.UtbkSnbt -> R.string.account_main_level_utbk
            MainLevel.Cpns -> R.string.account_main_level_cpns
            MainLevel.SekolahKedinasan -> R.string.account_main_level_kedinasan
            MainLevel.SmaSlta -> R.string.account_main_level_sma
            MainLevel.Smp -> R.string.account_main_level_smp
        }

@get:StringRes
internal val MainLevel.subtitleRes: Int
    get() =
        when (this) {
            MainLevel.UtbkSnbt -> R.string.account_main_level_utbk_subtitle
            MainLevel.Cpns -> R.string.account_main_level_cpns_subtitle
            MainLevel.SekolahKedinasan -> R.string.account_main_level_kedinasan_subtitle
            MainLevel.SmaSlta -> R.string.account_main_level_sma_subtitle
            MainLevel.Smp -> R.string.account_main_level_smp_subtitle
        }

@get:DrawableRes
internal val MainLevel.icon: Int
    get() =
        when (this) {
            MainLevel.UtbkSnbt -> GotestIcons.Linear.Building
            MainLevel.Cpns -> GotestIcons.Linear.Bank
            MainLevel.SekolahKedinasan -> GotestIcons.Linear.Buildings
            MainLevel.SmaSlta -> GotestIcons.Linear.Book
            MainLevel.Smp -> GotestIcons.Linear.BookOpened
        }
