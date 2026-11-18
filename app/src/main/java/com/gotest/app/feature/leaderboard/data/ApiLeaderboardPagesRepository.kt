package com.gotest.app.feature.leaderboard.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.leaderboard.domain.LeaderboardFilters
import com.gotest.app.feature.leaderboard.domain.LeaderboardPage
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.PackageBoardPage
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

/** Leaderboard routes of API module F; the address is the web's plus `/api/v1`. */
interface LeaderboardApi {
    /** Null filters are left out, as the web sends only the ones chosen. */
    @GET("api/v1/leaderboard")
    suspend fun leaderboard(
        @Query("scope") scope: String,
        @Query("category") category: Long?,
        @Query("subject") subject: Long?,
        @Query("period") period: String,
        @Query("limit") limit: Int,
    ): LeaderboardPage

    @GET("api/v1/leaderboard/packages/{package}")
    suspend fun tryoutBoard(
        @Path("package") packageId: Long,
    ): PackageBoardPage

    @GET("api/v1/leaderboard/latihan/{package}")
    suspend fun practiceBoard(
        @Path("package") packageId: Long,
    ): PackageBoardPage
}

@Singleton
class ApiLeaderboardPagesRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : LeaderboardPagesRepository {
        private val api = retrofit.create(LeaderboardApi::class.java)

        override suspend fun leaderboard(filters: LeaderboardFilters): Result<LeaderboardPage> =
            apiCall(json) {
                api.leaderboard(
                    filters.scope.query,
                    filters.category,
                    filters.subject,
                    filters.period.query,
                    filters.limit,
                )
            }

        override suspend fun tryoutBoard(packageId: Long): Result<PackageBoardPage> =
            apiCall(json) {
                api.tryoutBoard(packageId)
            }

        override suspend fun practiceBoard(packageId: Long): Result<PackageBoardPage> =
            apiCall(json) { api.practiceBoard(packageId) }
    }
