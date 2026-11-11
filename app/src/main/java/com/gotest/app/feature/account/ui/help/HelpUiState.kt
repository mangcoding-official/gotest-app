package com.gotest.app.feature.account.ui.help

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.account.domain.HelpCategory
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.domain.HelpFaq

sealed interface HelpUiState {
    data object Loading : HelpUiState

    data object Error : HelpUiState

    @Immutable
    data class Success(
        val query: String,
        val categories: List<HelpCategory>,
        /** FAQ matching [query]; empty shows "Pertanyaan tidak ditemukan" inside the FAQ card. */
        val faqs: List<HelpFaq>,
        val expandedFaqIds: Set<String>,
        val contacts: List<HelpContact>,
        val operatingHours: String,
    ) : HelpUiState
}

sealed interface HelpUiEvent {
    data object Retry : HelpUiEvent

    data class QueryChange(
        val value: String,
    ) : HelpUiEvent

    data class FaqToggle(
        val faqId: String,
    ) : HelpUiEvent
}
