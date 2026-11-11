package com.gotest.app.feature.account.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.account.domain.HelpCenter
import com.gotest.app.feature.account.domain.HelpFaq
import com.gotest.app.feature.account.domain.HelpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HelpViewModel
    @Inject
    constructor(
        helpRepository: HelpRepository,
    ) : ViewModel() {
        private val retryKey = MutableStateFlow(0)
        private val query = MutableStateFlow("")

        /** Null until the student toggles something: the first FAQ starts open, as in the design. */
        private val expandedFaqIds = MutableStateFlow<Set<String>?>(null)

        private val content: Flow<HelpUiState> =
            retryKey.flatMapLatest {
                helpRepository
                    .observeHelpCenter()
                    .map<HelpCenter, HelpUiState> { center ->
                        HelpUiState.Success(
                            query = "",
                            categories = center.categories,
                            faqs = center.faqs,
                            expandedFaqIds = setOfNotNull(center.faqs.firstOrNull()?.id),
                            contacts = center.contacts,
                            operatingHours = center.operatingHours,
                        )
                    }.onStart { emit(HelpUiState.Loading) }
                    .catch { emit(HelpUiState.Error) }
            }

        val uiState: StateFlow<HelpUiState> =
            combine(content, query, expandedFaqIds) { state, text, expanded ->
                if (state is HelpUiState.Success) {
                    state.copy(
                        query = text,
                        faqs = state.faqs.filter { it.matches(text) },
                        expandedFaqIds = expanded ?: state.expandedFaqIds,
                    )
                } else {
                    state
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HelpUiState.Loading)

        fun onEvent(event: HelpUiEvent) {
            when (event) {
                HelpUiEvent.Retry -> retryKey.update { it + 1 }
                is HelpUiEvent.QueryChange -> query.value = event.value
                is HelpUiEvent.FaqToggle -> toggle(event.faqId)
            }
        }

        private fun toggle(faqId: String) {
            val current = (uiState.value as? HelpUiState.Success)?.expandedFaqIds.orEmpty()
            expandedFaqIds.value = if (faqId in current) current - faqId else current + faqId
        }

        private fun HelpFaq.matches(text: String): Boolean {
            val needle = text.trim()
            return needle.isEmpty() ||
                question.contains(needle, ignoreCase = true) ||
                answer.contains(needle, ignoreCase = true)
        }
    }
