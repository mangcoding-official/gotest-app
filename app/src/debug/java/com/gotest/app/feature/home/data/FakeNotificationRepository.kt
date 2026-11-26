package com.gotest.app.feature.home.data

import com.gotest.app.core.data.fake.PackageIds
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.feature.home.domain.AppNotification
import com.gotest.app.feature.home.domain.NotificationRepository
import com.gotest.app.feature.home.domain.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Ages from the design: "10 mnt", "2 jam" and three times "1 hari".
private const val RESULT_AGE_MINUTES = 10L
private const val NEW_PACKAGE_AGE_MINUTES = 120L
private const val RANK_AGE_MINUTES = 1_440L
private const val TRIAL_AGE_MINUTES = 1_445L
private const val PRACTICE_AGE_MINUTES = 1_450L

/** The five notifications from the design, timed relative to app start. */
@Singleton
class FakeNotificationRepository
    @Inject
    constructor() : NotificationRepository {
        private val notifications = MutableStateFlow(seed(System.currentTimeMillis()))

        override fun observeNotifications(): Flow<List<AppNotification>> =
            notifications
                .map { list -> list.sortedByDescending { it.receivedAtEpochMillis } }
                .withFakeLatency()

        override fun observeUnreadCount(): Flow<Int> =
            notifications.map { list -> list.count { !it.isRead } }.withFakeLatency()

        override suspend fun markRead(notificationId: String) {
            notifications.update { list ->
                list.map { if (it.id == notificationId) it.copy(isRead = true) else it }
            }
        }

        override suspend fun markAllRead() {
            simulateLatency()
            notifications.update { list -> list.map { it.copy(isRead = true) } }
        }

        private fun seed(now: Long): List<AppNotification> {
            fun minutesAgo(minutes: Long) = now - TimeUnit.MINUTES.toMillis(minutes)
            return listOf(
                AppNotification(
                    id = "notif_tryout_result",
                    type = NotificationType.TryoutResult,
                    title = "Hasil tryout sudah keluar",
                    message = "Skor Simulasi UTBK-SNBT Session 2 kamu 540. Lihat pembahasannya sekarang.",
                    receivedAtEpochMillis = minutesAgo(RESULT_AGE_MINUTES),
                    isRead = false,
                    // Seeded history row in core/data/fake/DummyResults.
                    targetId = "att_tryout_utbk_2",
                ),
                AppNotification(
                    id = "notif_new_package",
                    type = NotificationType.NewPackage,
                    title = "Paket baru tersedia",
                    message = "Latihan TWK CPNS Session 2 sudah bisa kamu kerjakan.",
                    receivedAtEpochMillis = minutesAgo(NEW_PACKAGE_AGE_MINUTES),
                    isRead = false,
                    targetId = PackageIds.LATIHAN_TWK_CPNS,
                ),
                AppNotification(
                    id = "notif_rank_up",
                    type = NotificationType.RankUp,
                    title = "Peringkatmu naik!",
                    message = "Kamu naik 1 peringkat ke posisi #12 minggu ini.",
                    receivedAtEpochMillis = minutesAgo(RANK_AGE_MINUTES),
                    isRead = true,
                    targetId = null,
                ),
                AppNotification(
                    id = "notif_trial_ending",
                    type = NotificationType.TrialEnding,
                    title = "Masa trial akan berakhir",
                    message = "Paket Free Trial berakhir dalam 30 hari. Upgrade untuk akses penuh.",
                    receivedAtEpochMillis = minutesAgo(TRIAL_AGE_MINUTES),
                    isRead = true,
                    targetId = null,
                ),
                AppNotification(
                    id = "notif_practice_done",
                    type = NotificationType.PracticeCompleted,
                    title = "Latihan selesai",
                    message = "Kamu menyelesaikan Latihan September Session 1 dengan skor 31.",
                    receivedAtEpochMillis = minutesAgo(PRACTICE_AGE_MINUTES),
                    isRead = true,
                    targetId = "att_latihan_sept_1",
                ),
            )
        }
    }
