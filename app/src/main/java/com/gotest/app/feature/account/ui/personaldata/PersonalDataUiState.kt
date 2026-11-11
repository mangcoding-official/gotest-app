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
    ) : PersonalDataUiState
}

sealed interface PersonalDataUiEvent {
    data object Retry : PersonalDataUiEvent
}
