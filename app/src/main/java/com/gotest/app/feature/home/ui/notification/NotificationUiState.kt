package com.gotest.app.feature.home.ui.notification

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.home.domain.NotificationType
import java.time.LocalDate

/** Segmented control: Semua / Belum dibaca. */
enum class NotificationFilter { All, Unread }

/** Relative age shown on each row ("10 mnt", "2 jam", "1 hari"). */
sealed interface NotificationAge {
    data object JustNow : NotificationAge

    data class Minutes(
        val value: Int,
    ) : NotificationAge

    data class Hours(
        val value: Int,
    ) : NotificationAge

    data class Days(
        val value: Int,
    ) : NotificationAge
}

/** Group heading: "Hari ini", "Kemarin", or a date for anything older. */
sealed interface NotificationDay {
    /** Stable LazyColumn key. */
    val key: String

    data object Today : NotificationDay {
        override val key: String = "today"
    }

    data object Yesterday : NotificationDay {
        override val key: String = "yesterday"
    }

    data class On(
        val date: LocalDate,
    ) : NotificationDay {
        override val key: String get() = date.toString()
    }
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val age: NotificationAge,
    val isRead: Boolean,
    val targetId: String?,
)

@Immutable
data class NotificationGroup(
    val day: NotificationDay,
    val items: List<NotificationItem>,
)

sealed interface NotificationUiState {
    data object Loading : NotificationUiState

    data object Error : NotificationUiState

    /** Nothing to show under [filter] ("Notifikasi / Kosong"). */
    data class Empty(
        val filter: NotificationFilter,
        val unreadCount: Int,
    ) : NotificationUiState

    @Immutable
    data class Success(
        val filter: NotificationFilter,
        val unreadCount: Int,
        val groups: List<NotificationGroup>,
    ) : NotificationUiState
}

sealed interface NotificationUiEvent {
    data class SelectFilter(
        val filter: NotificationFilter,
    ) : NotificationUiEvent

    data class Open(
        val item: NotificationItem,
    ) : NotificationUiEvent

    data object MarkAllRead : NotificationUiEvent

    data object Retry : NotificationUiEvent
}

/** Where tapping a notification leads. */
sealed interface NotificationEffect {
    data class OpenResult(
        val attemptId: String,
    ) : NotificationEffect

    data class OpenPracticePackage(
        val packageId: String,
    ) : NotificationEffect

    data object OpenLeaderboard : NotificationEffect

    data object OpenUpgrade : NotificationEffect
}
