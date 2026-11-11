package com.gotest.app.feature.studypackage.ui.topiclist

import androidx.compose.runtime.Immutable
import com.gotest.app.core.model.Subject
import com.gotest.app.core.model.Topic
import com.gotest.app.feature.studypackage.ui.components.PracticeSheetState

@Immutable
data class TopicListUiState(
    val content: TopicListContent = TopicListContent.Loading,
    /** "Detail Latihan" sheet; null when closed. */
    val sheet: PracticeSheetState? = null,
)

sealed interface TopicListContent {
    data object Loading : TopicListContent

    data object Error : TopicListContent

    /** The subject has no topics yet; the app bar still names it. */
    @Immutable
    data class Empty(
        val subject: Subject,
        val levelLabel: String,
    ) : TopicListContent

    @Immutable
    data class Success(
        val subject: Subject,
        /** Short jenjang name for the subtitle ("SLTA • 12 Topik"). */
        val levelLabel: String,
        val topics: List<Topic>,
    ) : TopicListContent
}

sealed interface TopicListUiEvent {
    data object Retry : TopicListUiEvent

    /** The card's "Mulai Latihan" / "Latihan Lagi" button. */
    data class TopicActionClicked(
        val packageId: String,
    ) : TopicListUiEvent

    data object SheetDismissed : TopicListUiEvent

    data object SheetRetry : TopicListUiEvent

    data class SheetStartClicked(
        val packageId: String,
    ) : TopicListUiEvent
}

sealed interface TopicListEffect {
    data class StartExam(
        val packageId: String,
    ) : TopicListEffect
}
