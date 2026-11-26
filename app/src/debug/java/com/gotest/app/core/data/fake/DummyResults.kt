package com.gotest.app.core.data.fake

import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.PackageRanking
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.core.model.SectionResult
import com.gotest.app.core.model.TopicAccuracy
import java.time.LocalDate

internal object DummyResults {
    const val TRYOUT_ATTEMPT = "att_tryout_utbk_1"
    const val DEFAULT_PARTICIPANTS = 12_512
    const val DEFAULT_DURATION_SECONDS = 1_232

    private const val TRYOUT_DESCRIPTION =
        "Simulasi UTBK-SNBT dengan tujuh subtes, 160 soal, dan durasi 195 menit. " +
            "Skor merupakan hasil simulasi, bukan skor resmi UTBK-SNBT."

    val history: List<AttemptSummary> =
        listOf(
            AttemptSummary(
                "att_tryout_utbk_2",
                PackageIds.TRYOUT_UTBK_2,
                "Simulasi UTBK-SNBT Session 2",
                PackageType.Tryout,
                "UTBK-SNBT",
                LocalDate.of(2026, 10, 21),
                1_232,
                540,
            ),
            AttemptSummary(
                "att_latihan_sept_1",
                PackageIds.LATIHAN_SEPT_1,
                "Latihan September Session 1",
                PackageType.Practice,
                "UTBK-SNBT",
                LocalDate.of(2026, 10, 20),
                720,
                31,
            ),
            AttemptSummary(
                "att_latihan_logaritma",
                PackageIds.LATIHAN_LOGARITMA,
                "Latihan Logaritma Dasar",
                PackageType.Practice,
                "UTBK-SNBT",
                LocalDate.of(2026, 10, 18),
                540,
                7,
            ),
            AttemptSummary(
                TRYOUT_ATTEMPT,
                PackageIds.TRYOUT_UTBK_1,
                "Simulasi UTBK-SNBT Session 1",
                PackageType.Tryout,
                "UTBK-SNBT",
                LocalDate.of(2026, 9, 19),
                1_232,
                540,
            ),
            AttemptSummary(
                "att_latihan_twk_1",
                PackageIds.LATIHAN_TWK_CPNS,
                "Latihan TWK CPNS Session 1",
                PackageType.Practice,
                "CPNS",
                LocalDate.of(2026, 9, 12),
                900,
                14,
            ),
        )

    private val utbkSectionNames =
        listOf(
            "Penalaran Umum",
            "Pengetahuan dan Pemahaman Umum",
            "Pemahaman Bacaan dan Menulis",
            "Pengetahuan Kuantitatif",
            "Literasi Bahasa Indonesia",
            "Literasi Bahasa Inggris",
            "Penalaran Matematika",
        )

    /** Hasil Tryout from the design: 540 points, rank 12 of 12.512, 12/5/3. */
    val seededResults: List<AttemptResult> =
        listOf(
            AttemptResult(
                attemptId = TRYOUT_ATTEMPT,
                packageId = PackageIds.TRYOUT_UTBK_1,
                title = "Simulasi UTBK-SNBT September Session 1",
                description = TRYOUT_DESCRIPTION,
                examLabel = "UTBK-SNBT",
                type = PackageType.Tryout,
                score = 540,
                rank = 12,
                participants = DEFAULT_PARTICIPANTS,
                correct = 12,
                wrong = 5,
                empty = 3,
                topicAccuracy =
                    listOf(
                        TopicAccuracy("Matematika Dasar", 80),
                        TopicAccuracy("Logaritma", 50),
                        TopicAccuracy("Aljabar", 25),
                    ),
                weakestTopic = "Aljabar",
                sections = utbkSectionNames.map { SectionResult(it, correct = 20, wrong = 5, empty = 5) },
                completedOn = LocalDate.of(2026, 9, 19),
                durationSeconds = DEFAULT_DURATION_SECONDS,
            ),
        )

    /** Pembahasan strip in the design: 2 and 3 wrong, 7 skipped, the rest correct. */
    val seededReviews: Map<String, List<QuestionReview>> =
        mapOf(
            TRYOUT_ATTEMPT to
                questionsFor(PackageIds.TRYOUT_UTBK_1, 20).map { q ->
                    val answer =
                        when (q.number) {
                            2 -> QuestionAnswer(selectedKeys = setOf("B"))
                            3 -> QuestionAnswer(selectedKeys = setOf("D"))
                            7 -> QuestionAnswer()
                            else -> QuestionAnswer(selectedKeys = q.correctKeys, statementAnswers = q.correctStatements)
                        }
                    QuestionReview(q, answer)
                },
        )

    /** Results for seeded history rows that have no full record. */
    fun resultFor(
        attemptId: String,
        history: List<AttemptSummary>,
    ): AttemptResult? {
        val summary = history.firstOrNull { it.attemptId == attemptId } ?: return null
        val pkg = DummyCatalog.packageById(summary.packageId)
        return seededResults.first().copy(
            attemptId = attemptId,
            packageId = pkg.id,
            title = pkg.title,
            description = pkg.description,
            examLabel = pkg.examLabel,
            type = pkg.type,
            score = summary.score,
            completedOn = summary.completedOn,
            durationSeconds = summary.durationSeconds,
        )
    }

    val globalEntries =
        listOf(
            leaderboardEntry(1, "John S.", "avatar_john", 424, attempts = 152),
            leaderboardEntry(2, "Michael T.", "avatar_michael", 324, attempts = 148),
            leaderboardEntry(3, "Emily C.", "avatar_emily", 172, attempts = 145),
            leaderboardEntry(4, "Alex Thompson", "avatar_ardy", 124, attempts = 141, rankChange = 2),
            leaderboardEntry(5, "Mia Johnson", "avatar_nadia", 121, attempts = 138, rankChange = 1),
            leaderboardEntry(6, "Ethan Williams", "avatar_rizky", 118, attempts = 130, rankChange = -4),
            leaderboardEntry(
                12,
                "Gilang P.",
                "avatar_gilang",
                104,
                attempts = 98,
                rankChange = 1,
                isCurrentUser = true,
            ),
            leaderboardEntry(13, "Sophia Carter", "avatar_bima", 101, attempts = 96),
        )

    val packageEntries =
        listOf(
            leaderboardEntry(1, "John Stuwart", "avatar_john", 424, finishSeconds = 7_805),
            leaderboardEntry(2, "Michael T.", "avatar_michael", 324, finishSeconds = 8_621),
            leaderboardEntry(3, "Emily Carter", "avatar_emily", 172, finishSeconds = 9_612),
            leaderboardEntry(4, "Alex Thompson", "avatar_ardy", 124, finishSeconds = 9_161, rankChange = 2),
            leaderboardEntry(5, "Mia Johnson", "avatar_nadia", 121, finishSeconds = 9_310, rankChange = 1),
            leaderboardEntry(6, "Ethan Williams", "avatar_rizky", 118, finishSeconds = 9_482, rankChange = -4),
            leaderboardEntry(7, "Sophia Brown", "avatar_sinta", 116, finishSeconds = 9_655),
            leaderboardEntry(
                12,
                "Gilang P.",
                "avatar_gilang",
                104,
                finishSeconds = 10_280,
                rankChange = 1,
                isCurrentUser = true,
            ),
        )

    val packageRankings =
        listOf(
            PackageRanking(
                PackageIds.TRYOUT_UTBK_1,
                "Tryout UTBK-SNBT 2026 (Sesi 1)",
                "UTBK-SNBT",
                120,
                120,
                12,
                12_412,
                680,
                76,
                2,
            ),
            PackageRanking(
                PackageIds.TRYOUT_UTBK_2,
                "Tryout UTBK-SNBT 2026 (Sesi 2)",
                "UTBK-SNBT",
                120,
                120,
                7,
                9_820,
                712,
                81,
                1,
            ),
            PackageRanking(
                PackageIds.TRYOUT_CPNS_3,
                "Simulasi CPNS SKD Batch 3",
                "CPNS",
                120,
                120,
                40,
                5_115,
                402,
                64,
                -3,
            ),
        )
}
