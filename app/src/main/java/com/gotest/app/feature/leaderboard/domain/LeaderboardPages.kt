package com.gotest.app.feature.leaderboard.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Leaderboard pages of API module F (T-033, API-CONTRACTS "API Android modul F"): the same props as
 * the web's Leaderboard/Index and Leaderboard/Package, built by LeaderboardPageData.
 */

@Serializable
enum class LeaderboardScope(
    val query: String,
) {
    @SerialName("global")
    Global("global"),

    @SerialName("package")
    Package("package"),

    @SerialName("practice")
    Practice("practice"),
}

@Serializable
enum class LeaderboardPeriod(
    val query: String,
) {
    @SerialName("all")
    All("all"),

    @SerialName("7d")
    Week("7d"),

    @SerialName("30d")
    Month("30d"),

    @SerialName("year")
    Year("year"),
}

@Serializable
data class LeaderboardFilters(
    @SerialName("scope") val scope: LeaderboardScope = LeaderboardScope.Global,
    @SerialName("category") val category: Long? = null,
    @SerialName("subject") val subject: Long? = null,
    @SerialName("period") val period: LeaderboardPeriod = LeaderboardPeriod.All,
    /** 20 to 100; "Muat Lebih Banyak" asks again with 20 more. */
    @SerialName("limit") val limit: Int = DEFAULT_LIMIT,
) {
    companion object {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100
        const val STEP = 20
    }
}

@Serializable
data class NamedOption(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
)

@Serializable
data class LeaderboardOptions(
    @SerialName("categories") val categories: List<NamedOption> = emptyList(),
    @SerialName("subjects") val subjects: List<NamedOption> = emptyList(),
)

/** One row of the global board; [points] are on the 0–1,000 scale (DEC-093). */
@Serializable
data class LeaderboardEntry(
    @SerialName("rank") val rank: Int,
    @SerialName("userId") val userId: Long,
    @SerialName("name") val name: String,
    @SerialName("attempts") val attempts: Int = 0,
    @SerialName("points") val points: Int,
    /** Previous rank minus this rank: positive is up; null shows "-". */
    @SerialName("change") val change: Int? = 0,
    @SerialName("isCurrentUser") val isCurrentUser: Boolean = false,
)

/** A package card on the "Per Paket" (Try Out) and "Latihan" tabs. */
@Serializable
data class LeaderboardPackageCard(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("categoryName") val categoryName: String,
    @SerialName("questionCount") val questionCount: Int,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    /** Latihan only: "{n} Menit", "{n} detik per soal" or "Tanpa batas waktu". */
    @SerialName("timeLabel") val timeLabel: String? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("totalParticipants") val totalParticipants: Int = 0,
    @SerialName("points") val points: Int? = null,
    /** Latihan only: "Nilai" or "Skor Latihan"; Try Out cards say "Poin". */
    @SerialName("pointsLabel") val pointsLabel: String? = null,
    @SerialName("accuracy") val accuracy: Int? = null,
    @SerialName("change") val change: Int? = null,
)

/** The subjects of a category come from the active branches of the taxonomy (web `lib/taxonomy.ts`). */
@Serializable
data class TaxonomyHierarchy(
    @SerialName("groups") val groups: List<TaxonomyGroup> = emptyList(),
    @SerialName("branches") val branches: List<TaxonomyBranch> = emptyList(),
)

@Serializable
data class TaxonomyGroup(
    @SerialName("id") val id: Long,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String? = null,
    @SerialName("order_number") val orderNumber: Int = 0,
)

@Serializable
data class TaxonomyBranch(
    @SerialName("id") val id: Long,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("group_id") val groupId: Long? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("order_number") val orderNumber: Int = 0,
    @SerialName("master_status") val masterStatus: String? = null,
    @SerialName("category_status") val categoryStatus: String? = null,
)

/** `GET /leaderboard`. */
@Serializable
data class LeaderboardPage(
    @SerialName("filters") val filters: LeaderboardFilters = LeaderboardFilters(),
    @SerialName("options") val options: LeaderboardOptions = LeaderboardOptions(),
    @SerialName("top") val top: List<LeaderboardEntry> = emptyList(),
    @SerialName("rows") val rows: List<LeaderboardEntry> = emptyList(),
    /** This account's row in the whole ranking, even outside the rows shown. */
    @SerialName("current") val current: LeaderboardEntry? = null,
    @SerialName("packageCards") val packageCards: List<LeaderboardPackageCard> = emptyList(),
    @SerialName("taxonomyHierarchy") val taxonomyHierarchy: TaxonomyHierarchy = TaxonomyHierarchy(),
    @SerialName("totalParticipants") val totalParticipants: Int = 0,
    @SerialName("hasMore") val hasMore: Boolean = false,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class PackageBoardInfo(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("questionCount") val questionCount: Int,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    @SerialName("timeLabel") val timeLabel: String? = null,
    @SerialName("pointsLabel") val pointsLabel: String? = null,
    /** A windowed Try Out before its ranking opens. */
    @SerialName("rankingOpensAt") val rankingOpensAt: String? = null,
)

/** A row of one package's board: Try Out rows carry [durationUsed], Latihan rows [submittedAt]. */
@Serializable
data class PackageBoardEntry(
    @SerialName("rank") val rank: Int,
    @SerialName("userId") val userId: Long,
    @SerialName("name") val name: String,
    @SerialName("points") val points: Int,
    @SerialName("durationUsed") val durationUsed: Int? = null,
    @SerialName("submittedAt") val submittedAt: String? = null,
    @SerialName("change") val change: Int? = 0,
    @SerialName("isCurrentUser") val isCurrentUser: Boolean = false,
)

/** `GET /leaderboard/packages/{id}` (Try Out) and `GET /leaderboard/latihan/{id}` (`mode: practice`). */
@Serializable
data class PackageBoardPage(
    @SerialName("mode") val mode: String? = null,
    @SerialName("package") val board: PackageBoardInfo,
    @SerialName("top") val top: List<PackageBoardEntry> = emptyList(),
    @SerialName("rows") val rows: List<PackageBoardEntry> = emptyList(),
    @SerialName("current") val current: PackageBoardEntry? = null,
    @SerialName("totalParticipants") val totalParticipants: Int = 0,
) {
    val isPractice: Boolean get() = mode == "practice"
}

interface LeaderboardPagesRepository {
    suspend fun leaderboard(filters: LeaderboardFilters): Result<LeaderboardPage>

    suspend fun tryoutBoard(packageId: Long): Result<PackageBoardPage>

    suspend fun practiceBoard(packageId: Long): Result<PackageBoardPage>
}
