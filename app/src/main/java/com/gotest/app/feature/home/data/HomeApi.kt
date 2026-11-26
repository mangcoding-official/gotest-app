package com.gotest.app.feature.home.data

import com.gotest.app.feature.home.domain.ContinueAttempt
import com.gotest.app.feature.home.domain.DashboardAttempt
import com.gotest.app.feature.home.domain.DashboardBanner
import com.gotest.app.feature.home.domain.DashboardStats
import com.gotest.app.feature.home.domain.MitraTaskCard
import com.gotest.app.feature.home.domain.SchoolSpace
import com.gotest.app.feature.home.domain.SearchPage
import com.gotest.app.feature.home.domain.WeeklyLeaderboard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

/** Beranda and Cari of API module B (T-030, preflight API 4.5). */
interface HomeApi {
    @GET("api/v1/dashboard")
    suspend fun dashboard(): DashboardReply

    @GET("api/v1/search")
    suspend fun search(
        @Query("q") query: String,
    ): SearchPage
}

/** `GET /dashboard`; a package is read as the catalog's row of its mode (Latihan or Try Out). */
@Serializable
data class DashboardReply(
    @SerialName("schoolSpaces") val schoolSpaces: List<SchoolSpace> = emptyList(),
    @SerialName("mitraTasks") val mitraTasks: List<MitraTaskCard> = emptyList(),
    @SerialName("packages") val packages: List<JsonObject> = emptyList(),
    @SerialName("recentAttempts") val recentAttempts: List<DashboardAttempt> = emptyList(),
    @SerialName("stats") val stats: DashboardStats = DashboardStats(),
    @SerialName("continueAttempt") val continueAttempt: ContinueAttempt? = null,
    @SerialName("leaderboard") val leaderboard: WeeklyLeaderboard = WeeklyLeaderboard(),
    @SerialName("banner") val banner: DashboardBanner? = null,
)
