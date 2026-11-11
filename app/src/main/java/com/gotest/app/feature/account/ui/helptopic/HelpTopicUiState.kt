package com.gotest.app.feature.account.ui.helptopic

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.account.domain.HelpArticle
import com.gotest.app.feature.account.domain.HelpContact
import com.gotest.app.feature.account.domain.HelpTopicTag

sealed interface HelpTopicUiState {
    data object Loading : HelpTopicUiState

    /** Also used for an unknown topic id. */
    data object Error : HelpTopicUiState

    /** The topic exists but has no articles yet; it still offers "Hubungi Kami". */
    @Immutable
    data class Empty(
        val title: String,
        val subtitle: String,
        val supportContact: HelpContact,
    ) : HelpTopicUiState

    @Immutable
    data class Success(
        val title: String,
        val subtitle: String,
        val tags: List<HelpTopicTag>,
        /** Chip filter; null only when the topic has no tags. */
        val selectedTagId: String?,
        /** Articles carrying [selectedTagId]. */
        val articles: List<HelpArticle>,
        val expandedArticleIds: Set<String>,
        val supportContact: HelpContact,
    ) : HelpTopicUiState
}

sealed interface HelpTopicUiEvent {
    data object Retry : HelpTopicUiEvent

    data class TagSelect(
        val tagId: String,
    ) : HelpTopicUiEvent

    data class ArticleToggle(
        val articleId: String,
    ) : HelpTopicUiEvent
}
