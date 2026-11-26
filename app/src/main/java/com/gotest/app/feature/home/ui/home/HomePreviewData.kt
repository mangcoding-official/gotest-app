package com.gotest.app.feature.home.ui.home

import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.home.domain.ContinueAttempt
import com.gotest.app.feature.home.domain.DashboardAttempt
import com.gotest.app.feature.home.domain.DashboardPage
import com.gotest.app.feature.home.domain.DashboardStats
import com.gotest.app.feature.home.domain.MODE_TRYOUT
import com.gotest.app.feature.home.domain.MitraTaskCard
import com.gotest.app.feature.home.domain.NextSession
import com.gotest.app.feature.home.domain.SchoolSpace
import com.gotest.app.feature.home.domain.WeeklyLeaderboard
import com.gotest.app.feature.home.domain.WeeklyRow
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.tryout.domain.TryoutPackage

/** Sample Beranda for the home feature's @Preview functions and the debug build without the API. */
internal object HomePreviewData {
    val profile =
        HomeProfileSummary(firstName = "Gilang", avatarUrl = null, hasUnreadNotifications = false)

    val page =
        DashboardPage(
            schoolSpaces =
                listOf(
                    SchoolSpace(
                        name = "SMA Harapan Bangsa",
                        code = "SMAHB",
                        role = SchoolSpace.STUDENT,
                        nextSession = NextSession("Try Out Bulanan", "scheduled", "2026-12-05T08:00:00+07:00"),
                    ),
                ),
            mitraTasks = listOf(MitraTaskCard("KLG123", "Keluarga Pratama", todayCount = 2, lateCount = 1)),
            practicePackages =
                listOf(
                    PracticePackage(
                        id = 11,
                        name = "Latihan Penalaran Umum 1",
                        description = "Latihan penalaran induktif dan deduktif.",
                        categoryName = "UTBK-SNBT",
                        timerLabel = "Paket ini dapat dikerjakan tanpa Timer",
                        finishedCount = 2,
                        lastScore = 80.0,
                        lastFinishedAt = "2026-11-20T10:00:00+07:00",
                    ),
                    PracticePackage(
                        id = 12,
                        name = "Latihan Literasi Bahasa Indonesia",
                        categoryName = "UTBK-SNBT",
                        premium = true,
                    ),
                ),
            tryoutPackages =
                listOf(
                    TryoutPackage(id = 21, name = "Try Out UTBK-SNBT 1", categoryName = "UTBK-SNBT"),
                ),
            recentAttempts =
                listOf(
                    DashboardAttempt(
                        1,
                        "Latihan Penalaran Umum 1",
                        "UTBK-SNBT",
                        score = 80.0,
                        submittedAt = "2026-11-20T10:00:00+07:00",
                    ),
                    DashboardAttempt(
                        2,
                        "Try Out UTBK-SNBT 1",
                        "UTBK-SNBT",
                        MODE_TRYOUT,
                        612.0,
                        "Skor Try Out",
                        "2026-11-18T09:00:00+07:00",
                    ),
                ),
            stats = DashboardStats(questionsDone = 120, accuracy = 72, packagesDone = 4),
            continueAttempt =
                ContinueAttempt(
                    31,
                    packageName = "Latihan Penalaran Umum 2",
                    categoryName = "UTBK-SNBT",
                    questionCount = 20,
                    answeredCount = 8,
                ),
            leaderboard =
                WeeklyLeaderboard(
                    top =
                        listOf(
                            WeeklyRow(1, 7, "Sinta Dewi", 820),
                            WeeklyRow(2, 8, "Raka Putra", 790),
                            WeeklyRow(3, 9, "Ayu Lestari", 760),
                        ),
                    current = WeeklyRow(12, 3, "Gilang Pratama", 540),
                ),
        )

    /** A new student: nothing done, nothing in progress, no one ranked yet. */
    val newStudent =
        page.copy(
            schoolSpaces = emptyList(),
            mitraTasks = emptyList(),
            recentAttempts = emptyList(),
            stats = DashboardStats(),
            continueAttempt = null,
            leaderboard = WeeklyLeaderboard(),
        )

    fun ready(
        page: DashboardPage = this.page,
        packageType: PackageType = PackageType.Practice,
    ) = HomeUiState.Ready(profile = profile, page = page, packageType = packageType)
}
