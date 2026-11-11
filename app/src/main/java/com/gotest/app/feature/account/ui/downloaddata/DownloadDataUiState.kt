package com.gotest.app.feature.account.ui.downloaddata

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.feature.account.domain.AccountDataCategory

/** Checked by default on Unduh Data Akun (frame 23277); transactions are opt-in. */
internal val defaultDataCategories =
    setOf(
        AccountDataCategory.ProfileAndAccount,
        AccountDataCategory.PracticeHistory,
        AccountDataCategory.AssistantChats,
    )

sealed interface DownloadDataUiState {
    data object Loading : DownloadDataUiState

    data object Error : DownloadDataUiState

    @Immutable
    data class Success(
        /** Where the link is sent ("Dikirim ke …"). */
        val email: String,
        /** Finished attempts, for "N attempt, jawaban, dan skor". */
        val attemptCount: Int,
        val selected: Set<AccountDataCategory> = defaultDataCategories,
        val isSending: Boolean = false,
    ) : DownloadDataUiState {
        val canSubmit: Boolean get() = selected.isNotEmpty() && !isSending
    }
}

sealed interface DownloadDataUiEvent {
    data object Retry : DownloadDataUiEvent

    data class Toggle(
        val category: AccountDataCategory,
    ) : DownloadDataUiEvent

    data object Submit : DownloadDataUiEvent
}

sealed interface DownloadDataEffect {
    data object Requested : DownloadDataEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : DownloadDataEffect
}
