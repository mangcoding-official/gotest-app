package com.gotest.app.core.data.fake

import com.gotest.app.core.model.CategoryAccent
import com.gotest.app.core.model.Difficulty
import com.gotest.app.core.model.PackageProgress
import com.gotest.app.core.model.PackageTopic
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.core.model.Subject
import com.gotest.app.core.model.Topic
import com.gotest.app.core.model.TryoutSection
import java.time.LocalDate

internal object PackageIds {
    const val LATIHAN_SEPT_1 = "pkg_latihan_sept_1"
    const val LATIHAN_MTK_SMA = "pkg_latihan_mtk_sma"
    const val LATIHAN_TWK_CPNS = "pkg_latihan_twk_cpns_1"
    const val LATIHAN_IPA_SMP = "pkg_latihan_ipa_smp"
    const val LATIHAN_LOGARITMA = "pkg_latihan_logaritma"
    const val LATIHAN_ALJABAR = "pkg_latihan_aljabar_dasar"
    const val LATIHAN_ALJABAR_LINEAR = "pkg_latihan_aljabar_linear"
    const val TRYOUT_UTBK_1 = "pkg_tryout_utbk_sept_1"
    const val TRYOUT_UTBK_2 = "pkg_tryout_utbk_sept_2"
    const val TRYOUT_CPNS_3 = "pkg_tryout_cpns_skd_3"
    const val TOPIC_APP_DEV = "pkg_rpl_pengembangan_aplikasi"
    const val TOPIC_PROGRAMMING = "pkg_rpl_dasar_pemrograman"
}

private const val UTBK = "UTBK-SNBT"
private const val UTBK_TRYOUT_DESCRIPTION =
    "Simulasi UTBK-SNBT dengan tujuh subtes, 160 soal, dan durasi 195 menit. " +
        "Skor merupakan hasil simulasi, bukan skor resmi UTBK-SNBT."
private const val PRACTICE_SCORING = "+1 / 0"

private fun practice(
    id: String,
    title: String,
    description: String,
    examLabel: String,
    levelLabel: String,
    questionCount: Int = 20,
    progress: PackageProgress? = null,
    attempts: Int = 0,
    lastAttemptOn: LocalDate? = null,
    bestScore: Int? = null,
) = StudyPackage(
    id = id,
    title = title,
    description = description,
    type = PackageType.Practice,
    examLabel = examLabel,
    levelLabel = levelLabel,
    questionCount = questionCount,
    durationMinutes = 45,
    scoringLabel = PRACTICE_SCORING,
    progress = progress,
    attempts = attempts,
    lastAttemptOn = lastAttemptOn,
    bestScore = bestScore,
    rank = null,
    participants = null,
)

private fun tryout(
    id: String,
    title: String,
    examLabel: String = UTBK,
    description: String = UTBK_TRYOUT_DESCRIPTION,
    questionCount: Int = 160,
    durationMinutes: Int = 195,
    attempts: Int = 0,
    lastAttemptOn: LocalDate? = null,
    bestScore: Int? = null,
    rank: Int? = null,
    participants: Int? = null,
) = StudyPackage(
    id = id,
    title = title,
    description = description,
    type = PackageType.Tryout,
    examLabel = examLabel,
    levelLabel = "SMA",
    questionCount = questionCount,
    durationMinutes = durationMinutes,
    scoringLabel = "IRT",
    progress = null,
    attempts = attempts,
    lastAttemptOn = lastAttemptOn,
    bestScore = bestScore,
    rank = rank,
    participants = participants,
)

internal object DummyCatalog {
    private val oct12 = LocalDate.of(2025, 10, 12)

    val packages: List<StudyPackage> =
        listOf(
            practice(
                id = PackageIds.LATIHAN_SEPT_1,
                title = "Latihan September Session 1",
                description =
                    "Latihan persiapan UTBK-SNBT dengan 20 soal dan penilaian benar 1, salah 0. " +
                        "Cocok untuk mengukur pemahaman materi dasar.",
                examLabel = UTBK,
                levelLabel = "SMA",
                progress = PackageProgress(answered = 5, correct = 5, total = 20),
                attempts = 1,
            ),
            practice(
                id = PackageIds.LATIHAN_MTK_SMA,
                title = "Latihan Matematika SMA",
                description = "Latihan persiapan dengan 20 soal dan penilaian benar 1, salah 0.",
                examLabel = "SMA",
                levelLabel = "SMA",
                progress = PackageProgress(answered = 15, correct = 12, total = 20),
                attempts = 2,
            ),
            practice(
                id = PackageIds.LATIHAN_TWK_CPNS,
                title = "Latihan TWK CPNS Session 1",
                description = "Latihan Tes Wawasan Kebangsaan dengan 20 soal dan penilaian benar 1, salah 0.",
                examLabel = "CPNS",
                levelLabel = "CPNS",
                progress = PackageProgress(answered = 4, correct = 3, total = 20),
                attempts = 1,
            ),
            practice(
                id = PackageIds.LATIHAN_IPA_SMP,
                title = "Latihan IPA SMP",
                description = "Latihan IPA terpadu kelas 9 dengan 20 soal dan penilaian benar 1, salah 0.",
                examLabel = "SMP",
                levelLabel = "SMP",
                progress = PackageProgress(answered = 0, correct = 0, total = 20),
            ),
            practice(
                id = PackageIds.LATIHAN_LOGARITMA,
                title = "Latihan Logaritma Dasar",
                description = "Latihan sifat-sifat logaritma dengan 10 soal dan penilaian benar 1, salah 0.",
                examLabel = UTBK,
                levelLabel = "SMA",
                questionCount = 10,
                attempts = 1,
                lastAttemptOn = LocalDate.of(2026, 10, 18),
                bestScore = 7,
            ),
            practice(
                id = PackageIds.LATIHAN_ALJABAR,
                title = "Latihan Aljabar Dasar",
                description = "20 soal untuk memperkuat konsep persamaan dan fungsi.",
                examLabel = UTBK,
                levelLabel = "SMA",
            ),
            practice(
                id = PackageIds.LATIHAN_ALJABAR_LINEAR,
                title = "Latihan Aljabar Linear",
                description = "15 soal matriks dan sistem persamaan linear.",
                examLabel = UTBK,
                levelLabel = "SMA",
                questionCount = 15,
            ),
            practice(
                id = PackageIds.TOPIC_APP_DEV,
                title = "Pengembangan Aplikasi",
                description = UTBK_TRYOUT_DESCRIPTION,
                examLabel = UTBK,
                levelLabel = "SMA",
            ),
            practice(
                id = PackageIds.TOPIC_PROGRAMMING,
                title = "Dasar-Dasar Pemrograman",
                description = "Latihan algoritma, tipe data, dan struktur kontrol untuk siswa SMK RPL.",
                examLabel = "SMA",
                levelLabel = "SMA",
                attempts = 4,
                lastAttemptOn = oct12,
                bestScore = 412,
            ),
            tryout(
                id = PackageIds.TRYOUT_UTBK_1,
                title = "Simulasi UTBK-SNBT September Session 1",
                attempts = 4,
                lastAttemptOn = LocalDate.of(2026, 9, 19),
                bestScore = 540,
                rank = 12,
                participants = 12_512,
            ),
            tryout(
                id = PackageIds.TRYOUT_UTBK_2,
                title = "Simulasi UTBK-SNBT September Session 2",
                attempts = 1,
                lastAttemptOn = LocalDate.of(2026, 10, 21),
                bestScore = 540,
                rank = 7,
                participants = 9_820,
            ),
            tryout(
                id = PackageIds.TRYOUT_CPNS_3,
                title = "Simulasi CPNS SKD Batch 3",
                examLabel = "CPNS",
                description = "Simulasi Seleksi Kompetensi Dasar CPNS: TWK, TIU, dan TKP dalam 110 soal.",
                questionCount = 110,
                durationMinutes = 100,
                attempts = 1,
                rank = 40,
                participants = 5_115,
            ),
        )

    fun packageById(id: String): StudyPackage = packages.firstOrNull { it.id == id } ?: packages.first()

    val practiceTopics: List<PackageTopic> =
        listOf(
            PackageTopic("Matematika Dasar", 4, Difficulty.Easy),
            PackageTopic("Matematika Diskrit", 4, Difficulty.Medium),
            PackageTopic("Logaritma", 4, Difficulty.Medium),
            PackageTopic("Aljabar", 4, Difficulty.Hard),
            PackageTopic("Aljabar Linear", 4, Difficulty.Hard),
        )

    // Realistic UTBK-SNBT 2025 layout: 160 questions in 195 minutes.
    val utbkSections: List<TryoutSection> =
        listOf(
            TryoutSection(1, "Penalaran Umum", 30, 30),
            TryoutSection(2, "Pengetahuan dan Pemahaman Umum", 20, 15),
            TryoutSection(3, "Pemahaman Bacaan dan Menulis", 20, 25),
            TryoutSection(4, "Pengetahuan Kuantitatif", 20, 20),
            TryoutSection(5, "Literasi Bahasa Indonesia", 30, 42),
            TryoutSection(6, "Literasi Bahasa Inggris", 20, 20),
            TryoutSection(7, "Penalaran Matematika", 20, 43),
        )

    val practiceRules: List<String> =
        listOf(
            "Waktu berjalan otomatis setelah kamu menekan Mulai.",
            "Jawaban tersimpan otomatis, kamu bisa lanjut kapan saja.",
            "Pembahasan tersedia setelah latihan selesai.",
        )

    val tryoutRules: List<String> =
        listOf(
            "Timer wajib aktif per section.",
            "Saat waktu habis, section dikunci dan otomatis lanjut ke section berikutnya.",
            "Jawaban tersimpan otomatis di server.",
        )

    fun detail(packageId: String): StudyPackageDetail {
        val summary = packageById(packageId)
        return when (summary.type) {
            PackageType.Practice -> StudyPackageDetail(summary, practiceTopics, emptyList(), practiceRules)
            PackageType.Tryout -> StudyPackageDetail(summary, emptyList(), utbkSections, tryoutRules)
        }
    }

    val practiceCategories: List<StudyCategory> =
        listOf(
            StudyCategory(
                "cat_pt",
                "Perguruan Tinggi",
                "Mulai dari persiapan masuk perguruan tinggi dan latihan per jurusan",
                12,
                12,
                CategoryAccent.Yellow,
            ),
            StudyCategory(
                "cat_slta",
                "SLTA / Sederajat",
                "Latihan per mata pelajaran SMA, SMK, dan MA sesuai kurikulum terbaru",
                12,
                12,
                CategoryAccent.Grey,
            ),
            StudyCategory(
                "cat_sltp",
                "SLTP / Sederajat",
                "Persiapan ujian sekolah dan asesmen SMP serta MTs",
                12,
                12,
                CategoryAccent.Blue,
            ),
            StudyCategory(
                "cat_sd",
                "SD / Sederajat",
                "Latihan dasar membaca, berhitung, dan sains untuk SD dan MI",
                12,
                12,
                CategoryAccent.Red,
            ),
        )

    private val sltaSubjects =
        listOf(
            "IPA" to 0,
            "IPS" to 0,
            "Bahasa" to 0,
            "Matematika" to 0,
            "Rekayasa Perangkat Lunak" to 4,
            "DKV" to 0,
            "Bisnis Daring Pemasaran" to 0,
            "Teknik Komputer & Jaringan" to 0,
            "Ilmu Agama Islam" to 0,
        )

    /** The design only shows SLTA subjects; other categories reuse them as dummy data. */
    fun subjects(categoryId: String): List<Subject> =
        sltaSubjects.mapIndexed { index, (name, done) ->
            Subject(
                id = "${categoryId}_sub_$index",
                categoryId = categoryId,
                name = name,
                description = "Latihan $name per topik, mulai dari materi dasar hingga soal setara ujian.",
                completedPackages = done,
                totalPackages = 24,
                subjectCount = 12,
                packageCount = 12,
            )
        }

    fun subjectById(subjectId: String): Subject =
        subjects(subjectId.substringBefore("_sub_")).firstOrNull { it.id == subjectId }
            ?: subjects("cat_slta")[4]

    private val rplTopics =
        listOf(
            "Pengembangan Aplikasi",
            "Dasar-Dasar Pemrograman",
            "Sistem Basis Data",
            "Jaringan Komputer",
            "Analisis dan Desain Sistem",
            "Keamanan Jaringan",
            "Manajemen Proyek TI",
            "Pemrograman Web",
            "Pengujian Perangkat Lunak",
            "Desain User Interface",
            "Kewirausahaan TI",
            "Etika Profesi TI",
        )

    fun topics(subjectId: String): List<Topic> =
        rplTopics.mapIndexed { index, name ->
            val done = name == "Dasar-Dasar Pemrograman"
            Topic(
                id = "${subjectId}_topic_$index",
                subjectId = subjectId,
                name = name,
                topicCount = 12,
                packageId =
                    when (index) {
                        0 -> PackageIds.TOPIC_APP_DEV
                        1 -> PackageIds.TOPIC_PROGRAMMING
                        else -> PackageIds.LATIHAN_SEPT_1
                    },
                attempts = if (done) 4 else 0,
                lastScore = if (done) 412 else null,
                lastAttemptOn = if (done) oct12 else null,
            )
        }

    val levels: List<String> = listOf("UTBK-SNBT", "SMA", "CPNS", "SMP")
}
