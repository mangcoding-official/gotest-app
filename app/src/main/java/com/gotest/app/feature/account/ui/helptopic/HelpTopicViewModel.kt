package com.gotest.app.feature.account.ui.helptopic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gotest.app.feature.account.domain.HelpArticle
import com.gotest.app.feature.account.domain.HelpRepository
import com.gotest.app.feature.account.domain.HelpTopic
import com.gotest.app.navigation.HelpTopicDestination
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
class HelpTopicViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        helpRepository: HelpRepository,
    ) : ViewModel() {
        private val topicId = savedStateHandle.toRoute<HelpTopicDestination>().topicId
        private val retryKey = MutableStateFlow(0)

        /** Null until a chip is tapped: the topic's first chip, which covers every article. */
        private val selectedTagId = MutableStateFlow<String?>(null)

        /** Null until something is toggled: the first visible article starts open. */
        private val expandedArticleIds = MutableStateFlow<Set<String>?>(null)

        private val content: Flow<HelpTopicUiState> =
            retryKey.flatMapLatest {
                helpRepository
                    .observeTopic(topicId)
                    .map<HelpTopic, HelpTopicUiState> { topic -> topic.toUiState() }
                    .onStart { emit(HelpTopicUiState.Loading) }
                    .catch { emit(HelpTopicUiState.Error) }
            }

        val uiState: StateFlow<HelpTopicUiState> =
            combine(content, selectedTagId, expandedArticleIds) { state, tagId, expanded ->
                if (state is HelpTopicUiState.Success) {
                    val selected = tagId ?: state.selectedTagId
                    val visible = state.articles.filterBy(selected)
                    state.copy(
                        selectedTagId = selected,
                        articles = visible,
                        expandedArticleIds = expanded ?: setOfNotNull(visible.firstOrNull()?.id),
                    )
                } else {
                    state
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HelpTopicUiState.Loading)

        fun onEvent(event: HelpTopicUiEvent) {
            when (event) {
                HelpTopicUiEvent.Retry -> {
                    retryKey.update { it + 1 }
                }

                is HelpTopicUiEvent.TagSelect -> {
                    selectedTagId.value = event.tagId
                    // Let the first article of the new filter open, like on first load.
                    expandedArticleIds.value = null
                }

                is HelpTopicUiEvent.ArticleToggle -> {
                    val current = (uiState.value as? HelpTopicUiState.Success)?.expandedArticleIds.orEmpty()
                    expandedArticleIds.value =
                        if (event.articleId in current) current - event.articleId else current + event.articleId
                }
            }
        }

        private fun HelpTopic.toUiState(): HelpTopicUiState =
            if (articles.isEmpty()) {
                HelpTopicUiState.Empty(title = title, subtitle = subtitle, supportContact = supportContact)
            } else {
                HelpTopicUiState.Success(
                    title = title,
                    subtitle = subtitle,
                    tags = tags,
                    selectedTagId = tags.firstOrNull()?.id,
                    articles = articles,
                    expandedArticleIds = emptySet(),
                    supportContact = supportContact,
                )
            }

        private fun List<HelpArticle>.filterBy(tagId: String?): List<HelpArticle> =
            if (tagId == null) this else filter { tagId in it.tagIds }
    }
