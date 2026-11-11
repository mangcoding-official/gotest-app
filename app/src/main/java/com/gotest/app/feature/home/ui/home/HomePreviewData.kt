package com.gotest.app.feature.home.ui.home

import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageProgress
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.UserStats
import java.time.LocalDate

/** Literal sample data for the home feature's @Preview functions (values from the design). */
internal object HomePreviewData {
    private const val PRACTICE_QUESTIONS = 20
    private const val PRACTICE_MINUTES = 45
    private const val UTBK_DESCRIPTION =
        "Simulasi UTBK-SNBT dengan tujuh subtes, 160 soal, dan durasi 195 menit. " +
            "Skor merupakan hasil simulasi, bukan skor resmi UTBK-SNBT."

    val profile =
        HomeProfileSummary(firstName = "Gilang", avatarUrl = null, hasUnreadNotifications = true)

    val stats =
        UserStats(
            questionsAnswered = 100,
            accuracyPercent = 100,
            packagesCompleted = 412,
            questionsTrendPercent = -24,
            accuracyTrendPercent = 24,
            packagesTrendPercent = 24,
        )

    private fun pkg(
        id: String,
        title: String,
        type: PackageType,
        examLabel: String = "UTBK-SNBT",
        questionCount: Int = PRACTICE_QUESTIONS,
        durationMinutes: Int = PRACTICE_MINUTES,
        progress: PackageProgress? = null,
        attempts: Int = 0,
        lastAttemptOn: LocalDate? = null,
        bestScore: Int? = null,
    ) = StudyPackage(
        id = id,
        title = title,
        description = UTBK_DESCRIPTION,
        type = type,
        examLabel = examLabel,
        levelLabel = "SMA",
        questionCount = questionCount,
        durationMinutes = durationMinutes,
        scoringLabel = "+1 / 0",
        progress = progress,
        attempts = attempts,
        lastAttemptOn = lastAttemptOn,
        bestScore = bestScore,
        rank = null,
        participants = null,
    )

    val inProgress =
        pkg(
            id = "pkg_latihan_sept_1",
            title = "Latihan September Session 1",
            type = PackageType.Practice,
            progress = PackageProgress(answered = 5, correct = 5, total = 20),
            attempts = 1,
        )

    val practicePackages =
        listOf(
            pkg(id = "pkg_app_dev", title = "Pengembangan Aplikasi", type = PackageType.Practice),
            pkg(
                id = "pkg_programming",
                title = "Dasar-Dasar Pemrograman",
                type = PackageType.Practice,
                examLabel = "SMA",
                attempts = 4,
                lastAttemptOn = LocalDate.of(2025, 10, 12),
                bestScore = 412,
            ),
        )

    val tryoutPackages =
        listOf(
            pkg(
                id = "pkg_tryout_1",
                title = "Simulasi UTBK-SNBT September Session 1",
                type = PackageType.Tryout,
                questionCount = 160,
                durationMinutes = 195,
                attempts = 4,
            ),
            pkg(
                id = "pkg_tryout_2",
                title = "Simulasi UTBK-SNBT September Session 2",
                type = PackageType.Tryout,
                questionCount = 160,
                durationMinutes = 195,
            ),
        )

    val attempts =
        List(size = 3) { index ->
            AttemptSummary(
                attemptId = "att_preview_$index",
                packageId = "pkg_latihan_sept_1",
                title = "Latihan September Session 1",
                type = PackageType.Practice,
                examLabel = "UTBK-SNBT",
                completedOn = LocalDate.of(2026, 10, 21),
                durationSeconds = 720,
                score = 31,
            )
        }

    fun recommendations(selectedType: PackageType = PackageType.Practice) =
        HomeRecommendations(selectedType = selectedType, practice = practicePackages, tryout = tryoutPackages)

    fun success(recommendedType: PackageType = PackageType.Practice) =
        HomeUiState.Success(
            profile = profile,
            stats = stats,
            inProgress = inProgress,
            recommendations = recommendations(recommendedType),
            activity = HomeActivity(selectedType = PackageType.Practice, attempts = attempts),
        )

    val empty =
        HomeUiState.Empty(
            profile = profile,
            recommendations = recommendations(),
            activityType = PackageType.Practice,
        )
}
