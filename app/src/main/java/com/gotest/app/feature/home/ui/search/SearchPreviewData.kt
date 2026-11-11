package com.gotest.app.feature.home.ui.search

import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.home.domain.HelpArticle
import com.gotest.app.feature.home.domain.SearchAttempt
import java.time.LocalDate

/** Literal sample data for the search @Preview functions ("utbk" from the design). */
internal object SearchPreviewData {
    private fun tryout(
        id: String,
        title: String,
    ) = StudyPackage(
        id = id,
        title = title,
        description = "Simulasi UTBK-SNBT dengan tujuh subtes.",
        type = PackageType.Tryout,
        examLabel = "UTBK-SNBT",
        levelLabel = "SMA",
        questionCount = 200,
        durationMinutes = 120,
        scoringLabel = "IRT",
        progress = null,
        attempts = 0,
        lastAttemptOn = null,
        bestScore = null,
        rank = null,
        participants = null,
    )

    val success =
        SearchUiState.Success(
            query = "utbk",
            filter = SearchFilter.All,
            tryouts =
                listOf(
                    tryout(id = "pkg_1", title = "Simulasi UTBK-SNBT Session 1"),
                    tryout(id = "pkg_2", title = "Simulasi UTBK-SNBT Session 2"),
                ),
            practices = emptyList(),
            topics =
                listOf(
                    "Penalaran Umum",
                    "Literasi B. Indonesia",
                    "Literasi B. Inggris",
                    "Penalaran Matematika",
                    "Pengetahuan Kuantitatif",
                    "PPU",
                ),
            attempts =
                listOf(
                    SearchAttempt(
                        attemptId = "att_1",
                        title = "Tryout UTBK-SNBT 2025",
                        type = PackageType.Tryout,
                        completedOn = LocalDate.of(2025, 9, 12),
                        score = 612,
                        rank = 12,
                    ),
                ),
            articles =
                listOf(
                    HelpArticle(id = "help_1", title = "Panduan Mengerjakan Tryout UTBK-SNBT", readMinutes = 5),
                    HelpArticle(id = "help_2", title = "Strategi dan Tips UTBK", readMinutes = 4),
                ),
        )
}
