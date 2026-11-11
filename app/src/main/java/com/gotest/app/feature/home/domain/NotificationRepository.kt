package com.gotest.app.feature.home.domain

import kotlinx.coroutines.flow.Flow

/** Kind of notification; decides the icon on Notifikasi and where a tap leads. */
enum class NotificationType {
    /** A tryout was graded; [AppNotification.targetId] is the attempt id. */
    TryoutResult,

    /** A new package is available; [AppNotification.targetId] is the package id. */
    NewPackage,

    /** The student moved up the global leaderboard. */
    RankUp,

    /** The free trial is about to end. */
    TrialEnding,

    /** A latihan was finished; [AppNotification.targetId] is the attempt id. */
    PracticeCompleted,
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val receivedAtEpochMillis: Long,
    val isRead: Boolean,
    /** Attempt or package the notification is about; null when the type needs none. */
    val targetId: String?,
)

/** Inbox behind the bell on Beranda and the Notifikasi screen. */
interface NotificationRepository {
    /** Newest first. */
    fun observeNotifications(): Flow<List<AppNotification>>

    /** Drives the red dot on Beranda's bell. */
    fun observeUnreadCount(): Flow<Int>

    suspend fun markRead(notificationId: String)

    suspend fun markAllRead()
}
