package com.gotest.app.feature.account.ui.personaldata

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.account.domain.PersonalDetails

sealed interface PersonalDataUiState {
    data object Loading : PersonalDataUiState

    data object Error : PersonalDataUiState

    @Immutable
    data class Success(
        val fullName: String,
        val email: String,
        val details: PersonalDetails,
        /** False for a child account: its parent or teacher keeps the details (Aturan 11.30). */
        val canEdit: Boolean = true,
    ) : PersonalDataUiState
}

sealed interface PersonalDataUiEvent {
    data object Retry : PersonalDataUiEvent
}
