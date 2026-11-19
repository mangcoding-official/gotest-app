package com.gotest.app.feature.history.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.InputStream

/*
 * Riwayat of API module F (T-033): the same props as the web's History/Index, built by
 * StudentHistoryData. 20 results a page; the app appends the next page like "Tampilkan lebih banyak".
 */

@Serializable
enum class HistoryMode(
    val query: String,
) {
    @SerialName("all")
    All("all"),

    @SerialName("practice")
    Practice("practice"),

    @SerialName("tryout")
    Tryout("tryout"),
}

@Serializable
enum class HistorySort(
    val query: String,
) {
    @SerialName("newest")
    Newest("newest"),

    @SerialName("oldest")
    Oldest("oldest"),
}

@Serializable
data class HistoryFilters(
    @SerialName("q") val q: String = "",
    @SerialName("mode") val mode: HistoryMode = HistoryMode.All,
    @SerialName("category") val category: Long? = null,
    @SerialName("sort") val sort: HistorySort = HistorySort.Newest,
)

@Serializable
data class HistoryCategory(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
)

@Serializable
data class HistoryPagination(
    @SerialName("nextPage") val nextPage: Int? = null,
    @SerialName("total") val total: Int = 0,
)

/** The whole history, whatever the filters (web `StudentStatCards`). */
@Serializable
data class HistoryStats(
    @SerialName("questionsDone") val questionsDone: Int = 0,
    @SerialName("accuracy") val accuracy: Int = 0,
    @SerialName("packagesDone") val packagesDone: Int = 0,
)

@Serializable
data class HistorySection(
    @SerialName("name") val name: String,
    @SerialName("correct") val correct: Int = 0,
    @SerialName("wrong") val wrong: Int = 0,
    @SerialName("empty") val empty: Int = 0,
)

/** One finished attempt; the detail adds [packageId] and [sections]. */
@Serializable
data class HistoryResult(
    @SerialName("id") val id: Long,
    @SerialName("packageName") val packageName: String,
    @SerialName("sessionTitle") val sessionTitle: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("categoryName") val categoryName: String,
    /** `tryout` or `practice`. */
    @SerialName("mode") val mode: String,
    @SerialName("submittedAt") val submittedAt: String? = null,
    @SerialName("durationSeconds") val durationSeconds: Int = 0,
    @SerialName("durationLabel") val durationLabel: String,
    @SerialName("accuracy") val accuracy: Int = 0,
    @SerialName("score") val score: Double = 0.0,
    /** Latihan only: "Nilai" or "Skor Latihan". */
    @SerialName("scoreLabel") val scoreLabel: String? = null,
    /** Try Out only. */
    @SerialName("ranked") val ranked: Boolean? = null,
    @SerialName("disqualified") val disqualified: Boolean? = null,
    @SerialName("packageId") val packageId: Long? = null,
    @SerialName("sections") val sections: List<HistorySection> = emptyList(),
) {
    val isTryout: Boolean get() = mode == MODE_TRYOUT

    private companion object {
        const val MODE_TRYOUT = "tryout"
    }
}

/** `GET /riwayat`. */
@Serializable
data class HistoryPage(
    @SerialName("categories") val categories: List<HistoryCategory> = emptyList(),
    @SerialName("filters") val filters: HistoryFilters = HistoryFilters(),
    @SerialName("attempts") val attempts: List<HistoryResult> = emptyList(),
    @SerialName("pagination") val pagination: HistoryPagination = HistoryPagination(),
    @SerialName("stats") val stats: HistoryStats = HistoryStats(),
)

@Serializable
data class HistoryRankingRow(
    @SerialName("rank") val rank: Int,
    @SerialName("name") val name: String,
    @SerialName("score") val score: Double,
    @SerialName("submittedAt") val submittedAt: String? = null,
    @SerialName("durationLabel") val durationLabel: String,
)

/** `GET /riwayat/attempts/{attempt}`: the result and the package's top 5 (Try Out). */
@Serializable
data class HistoryDetail(
    @SerialName("attempt") val attempt: HistoryResult,
    @SerialName("ranking") val ranking: List<HistoryRankingRow> = emptyList(),
)

interface HistoryPagesRepository {
    suspend fun history(
        filters: HistoryFilters,
        page: Int,
    ): Result<HistoryPage>

    suspend fun detail(attemptId: Long): Result<HistoryDetail>

    /** `riwayat-pengerjaan.csv`: the whole history in WIB, fetched with the sign-in token (API-K10). */
    suspend fun export(): Result<InputStream>
}
