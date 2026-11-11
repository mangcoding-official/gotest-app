package com.gotest.app.feature.home.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.home.domain.AppNotification
import com.gotest.app.feature.home.domain.NotificationRepository
import com.gotest.app.feature.home.domain.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) : ViewModel() {
        private val filter = MutableStateFlow(NotificationFilter.All)
        private val retryCount = MutableStateFlow(0)

        private val _effects = Channel<NotificationEffect>(Channel.BUFFERED)
        val effects: Flow<NotificationEffect> = _effects.receiveAsFlow()

        val uiState: StateFlow<NotificationUiState> =
            retryCount
                .flatMapLatest { attempt ->
                    val state = combine(notificationRepository.observeNotifications(), filter, ::toUiState)
                    val withSpinner = if (attempt > 0) state.onStart { emit(NotificationUiState.Loading) } else state
                    withSpinner.catch { emit(NotificationUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NotificationUiState.Loading)

        fun onEvent(event: NotificationUiEvent) {
            when (event) {
                is NotificationUiEvent.SelectFilter -> filter.value = event.filter
                is NotificationUiEvent.Open -> open(event.item)
                NotificationUiEvent.MarkAllRead -> viewModelScope.launch { notificationRepository.markAllRead() }
                NotificationUiEvent.Retry -> retryCount.update { it + 1 }
            }
        }

        private fun open(item: NotificationItem) {
            viewModelScope.launch {
                effectFor(item)?.let { _effects.send(it) }
                if (!item.isRead) notificationRepository.markRead(item.id)
            }
        }

        private fun effectFor(item: NotificationItem): NotificationEffect? =
            when (item.type) {
                NotificationType.TryoutResult,
                NotificationType.PracticeCompleted,
                -> item.targetId?.let(NotificationEffect::OpenResult)

                NotificationType.NewPackage -> item.targetId?.let(NotificationEffect::OpenPracticePackage)

                NotificationType.RankUp -> NotificationEffect.OpenLeaderboard

                NotificationType.TrialEnding -> NotificationEffect.OpenUpgrade
            }

        private fun toUiState(
            notifications: List<AppNotification>,
            filter: NotificationFilter,
        ): NotificationUiState {
            val unreadCount = notifications.count { !it.isRead }
            val visible =
                if (filter == NotificationFilter.Unread) notifications.filter { !it.isRead } else notifications
            if (visible.isEmpty()) return NotificationUiState.Empty(filter, unreadCount)

            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val today = LocalDate.now(zone)
            val groups =
                visible
                    .sortedByDescending { it.receivedAtEpochMillis }
                    .groupBy { Instant.ofEpochMilli(it.receivedAtEpochMillis).atZone(zone).toLocalDate() }
                    .map { (date, items) ->
                        NotificationGroup(
                            day =
                                when (date) {
                                    today -> NotificationDay.Today
                                    today.minusDays(1) -> NotificationDay.Yesterday
                                    else -> NotificationDay.On(date)
                                },
                            items = items.map { it.toItem(now) },
                        )
                    }
            return NotificationUiState.Success(filter, unreadCount, groups)
        }

        private fun AppNotification.toItem(now: Instant) =
            NotificationItem(
                id = id,
                type = type,
                title = title,
                message = message,
                age = ageOf(Duration.between(Instant.ofEpochMilli(receivedAtEpochMillis), now)),
                isRead = isRead,
                targetId = targetId,
            )

        private fun ageOf(elapsed: Duration): NotificationAge {
            val minutes = elapsed.toMinutes()
            return when {
                minutes < 1 -> {
                    NotificationAge.JustNow
                }

                minutes < MINUTES_PER_HOUR -> {
                    NotificationAge.Minutes(minutes.toInt())
                }

                minutes < MINUTES_PER_HOUR * HOURS_PER_DAY -> {
                    NotificationAge.Hours(
                        (minutes / MINUTES_PER_HOUR).toInt(),
                    )
                }

                else -> {
                    NotificationAge.Days((minutes / (MINUTES_PER_HOUR * HOURS_PER_DAY)).toInt())
                }
            }
        }
    }
