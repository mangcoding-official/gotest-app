package com.gotest.app.feature.studypackage.ui.components

import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.CategoryAccent
import com.gotest.app.core.model.Difficulty
import com.gotest.app.core.model.PackageProgress
import com.gotest.app.core.model.PackageTopic
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.SeasonalTryout
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.core.model.Subject
import com.gotest.app.core.model.Topic
import com.gotest.app.core.model.TryoutSection
import com.gotest.app.feature.studypackage.domain.SavedPackage
import java.time.LocalDate

/** Sample data for this feature's @Preview functions only. */
internal object StudyPackagePreviewData {
    private const val DESCRIPTION = "Mulai dari persiapan masuk perguruan tinggi dan latihan per jurusan"
    private val lastAttempt = LocalDate.of(2025, 10, 12)

    val categories: List<StudyCategory> =
        listOf(
            category(id = "cat_pt", name = "Perguruan Tinggi", accent = CategoryAccent.Yellow),
            category(id = "cat_slta", name = "SLTA / Sederajat", accent = CategoryAccent.Grey),
            category(id = "cat_sltp", name = "SLTP / Sederajat", accent = CategoryAccent.Blue),
            category(id = "cat_sd", name = "SD / Sederajat", accent = CategoryAccent.Red),
        )

    val practicePackage: StudyPackage =
        StudyPackage(
            id = "pkg_sept_1",
            title = "Latihan September Session 1",
            description = "Latihan persiapan UTBK-SNBT dengan 20 soal dan penilaian benar 1, salah 0.",
            type = PackageType.Practice,
            examLabel = "UTBK-SNBT",
            levelLabel = "SMA",
            questionCount = 20,
            durationMinutes = 45,
            scoringLabel = "+1 / 0",
            progress = PackageProgress(answered = 5, correct = 5, total = 20),
            attempts = 1,
            lastAttemptOn = null,
            bestScore = null,
            rank = null,
            participants = null,
        )

    val packages: List<StudyPackage> =
        listOf(
            practicePackage,
            practicePackage.copy(
                id = "pkg_mtk",
                title = "Latihan Matematika SMA",
                examLabel = "SMA",
                progress = PackageProgress(answered = 20, correct = 15, total = 20),
            ),
            practicePackage.copy(
                id = "pkg_ipa",
                title = "Latihan IPA SMP",
                examLabel = "SMP",
                progress = PackageProgress(answered = 0, correct = 0, total = 20),
                attempts = 0,
            ),
        )

    val tryoutPackage: StudyPackage =
        practicePackage.copy(
            id = "pkg_tryout_1",
            title = "Simulasi UTBK-SNBT September Session 1",
            description = "Simulasi UTBK-SNBT dengan tujuh subtes, 160 soal, dan durasi 195 menit.",
            type = PackageType.Tryout,
            questionCount = 160,
            durationMinutes = 195,
            scoringLabel = "IRT",
            progress = null,
            attempts = 0,
        )

    val practiceDetail: StudyPackageDetail =
        StudyPackageDetail(
            summary = practicePackage,
            topics =
                listOf(
                    PackageTopic(name = "Literasi Bahasa Indonesia", questionCount = 8, difficulty = Difficulty.Medium),
                    PackageTopic(name = "Penalaran Umum", questionCount = 6, difficulty = Difficulty.Hard),
                    PackageTopic(name = "Matematika", questionCount = 6, difficulty = Difficulty.Easy),
                ),
            sections = emptyList(),
            rules =
                listOf(
                    "Waktu berjalan otomatis setelah kamu menekan Mulai.",
                    "Jawaban tersimpan otomatis, kamu bisa lanjut kapan saja.",
                    "Pembahasan tersedia setelah latihan selesai.",
                ),
        )

    val tryoutDetail: StudyPackageDetail =
        StudyPackageDetail(
            summary = tryoutPackage,
            topics = emptyList(),
            sections =
                listOf(
                    TryoutSection(order = 1, name = "Penalaran Umum", questionCount = 30, durationMinutes = 30),
                    TryoutSection(
                        order = 2,
                        name = "Pengetahuan Kuantitatif",
                        questionCount = 20,
                        durationMinutes = 20,
                    ),
                    TryoutSection(order = 3, name = "Penalaran Matematika", questionCount = 20, durationMinutes = 43),
                ),
            rules =
                listOf(
                    "Timer wajib aktif per section.",
                    "Saat waktu habis, section dikunci dan otomatis lanjut ke section berikutnya.",
                ),
        )

    /** The three cards of "Paket / Tersimpan"; the latihan has no timer, as in the design. */
    val savedPackages: List<SavedPackage> =
        listOf(
            SavedPackage(studyPackage = tryoutPackage, savedOn = LocalDate.of(2026, 10, 28)),
            SavedPackage(
                studyPackage = practicePackage.copy(title = "Latihan September Session 01", durationMinutes = null),
                savedOn = LocalDate.of(2026, 10, 26),
            ),
            SavedPackage(
                studyPackage =
                    tryoutPackage.copy(
                        id = "pkg_tryout_cpns",
                        title = "Simulasi CPNS SKD Batch 3",
                        examLabel = "CPNS",
                        questionCount = 110,
                        durationMinutes = 100,
                    ),
                savedOn = LocalDate.of(2026, 10, 20),
            ),
        )

    val seasonalTryout: SeasonalTryout =
        SeasonalTryout(
            title = "Seasonal Tryout",
            description = "Ini adalah tryout yang dibatasi pada periode tertentu saja.",
            endsAtEpochMillis = 0L,
            packages =
                listOf(
                    tryoutPackage.copy(id = "seasonal_1", title = "Pengembangan Aplikasi"),
                    tryoutPackage.copy(
                        id = "seasonal_2",
                        title = "Dasar-Dasar Pemrograman",
                        attempts = 4,
                        lastAttemptOn = lastAttempt,
                        bestScore = 412,
                        rank = 4,
                        participants = 124,
                    ),
                ),
        )

    val history: List<AttemptSummary> =
        listOf(
            AttemptSummary(
                attemptId = "att_1",
                packageId = "pkg_sept_1",
                title = "Latihan September Session 1",
                type = PackageType.Practice,
                examLabel = "UTBK-SNBT",
                completedOn = LocalDate.of(2026, 10, 21),
                durationSeconds = 1_800,
                score = 15,
            ),
            AttemptSummary(
                attemptId = "att_2",
                packageId = "pkg_mtk",
                title = "Latihan Matematika SMA",
                type = PackageType.Practice,
                examLabel = "SMA",
                completedOn = LocalDate.of(2026, 10, 18),
                durationSeconds = 1_500,
                score = 12,
            ),
        )

    val subjects: List<Subject> =
        listOf("IPA", "IPS", "Bahasa", "Rekayasa Perangkat Lunak").mapIndexed { index, name ->
            Subject(
                id = "sub_$index",
                categoryId = "cat_slta",
                name = name,
                description = DESCRIPTION,
                completedPackages = index,
                totalPackages = 24,
                subjectCount = 12,
                packageCount = 12,
            )
        }

    val topics: List<Topic> =
        listOf(
            topic(id = "topic_0", name = "Pengembangan Aplikasi"),
            topic(id = "topic_1", name = "Dasar-Dasar Pemrograman").copy(
                attempts = 4,
                lastScore = 412,
                lastAttemptOn = lastAttempt,
            ),
            topic(id = "topic_2", name = "Sistem Basis Data"),
        )

    private fun category(
        id: String,
        name: String,
        accent: CategoryAccent,
    ) = StudyCategory(
        id = id,
        name = name,
        description = DESCRIPTION,
        subCategoryCount = 12,
        packageCount = 12,
        accent = accent,
    )

    private fun topic(
        id: String,
        name: String,
    ) = Topic(
        id = id,
        subjectId = "sub_3",
        name = name,
        topicCount = 12,
        packageId = "pkg_sept_1",
        attempts = 0,
        lastScore = null,
        lastAttemptOn = null,
    )
}
