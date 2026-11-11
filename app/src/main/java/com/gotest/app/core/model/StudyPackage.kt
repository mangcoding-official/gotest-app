package com.gotest.app.core.model

import java.time.LocalDate

/** "Latihan" packages are untimed practice; "Tryout" packages simulate a timed exam. */
enum class PackageType { Practice, Tryout }

enum class Difficulty { Easy, Medium, Hard }

/** Tone of the small colour bar on category cards. */
enum class CategoryAccent { Yellow, Grey, Blue, Red }

/** Top-level level grouping ("Perguruan Tinggi", "SLTA / Sederajat", …). */
data class StudyCategory(
    val id: String,
    val name: String,
    val description: String,
    val subCategoryCount: Int,
    val packageCount: Int,
    val accent: CategoryAccent,
)

/** A subject inside a category ("IPA", "Rekayasa Perangkat Lunak", …). */
data class Subject(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val completedPackages: Int,
    val totalPackages: Int,
    val subjectCount: Int,
    val packageCount: Int,
)

/** A topic inside a subject; each topic opens one practice package. */
data class Topic(
    val id: String,
    val subjectId: String,
    val name: String,
    val topicCount: Int,
    val packageId: String,
    val attempts: Int,
    val lastScore: Int?,
    val lastAttemptOn: LocalDate?,
)

data class StudyPackage(
    val id: String,
    val title: String,
    val description: String,
    val type: PackageType,
    /** Exam the package prepares for, shown as a chip: "UTBK-SNBT", "CPNS", … */
    val examLabel: String,
    /** School level used by the jenjang filter: "SMA", "SMP", … */
    val levelLabel: String,
    val questionCount: Int,
    /** Null when the package has no timer. */
    val durationMinutes: Int?,
    val scoringLabel: String,
    val progress: PackageProgress?,
    val attempts: Int,
    val lastAttemptOn: LocalDate?,
    val bestScore: Int?,
    val rank: Int?,
    val participants: Int?,
)

data class PackageProgress(
    val answered: Int,
    val correct: Int,
    val total: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else answered.toFloat() / total
}

data class PackageTopic(
    val name: String,
    val questionCount: Int,
    val difficulty: Difficulty?,
)

data class TryoutSection(
    val order: Int,
    val name: String,
    val questionCount: Int,
    val durationMinutes: Int,
)

/** Everything the detail screens and the "Detail Latihan" sheet show. */
data class StudyPackageDetail(
    val summary: StudyPackage,
    val topics: List<PackageTopic>,
    val sections: List<TryoutSection>,
    val rules: List<String>,
)

/** Limited-time tryout promoted on the Tryout tab, with a live countdown. */
data class SeasonalTryout(
    val title: String,
    val description: String,
    val endsAtEpochMillis: Long,
    val packages: List<StudyPackage>,
)
