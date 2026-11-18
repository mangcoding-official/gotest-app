package com.gotest.app.feature.history.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.history.domain.HistoryDetail
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryMode
import com.gotest.app.feature.history.domain.HistoryPage
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import com.gotest.app.feature.history.domain.HistorySort
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Riwayat routes of API module F. */
interface HistoryApi {
    /** Only the filters that are not the default are sent, as on the web. */
    @GET("api/v1/riwayat")
    suspend fun history(
        @Query("q") q: String?,
        @Query("mode") mode: String?,
        @Query("category") category: Long?,
        @Query("sort") sort: String?,
        @Query("page") page: Int?,
    ): HistoryPage

    @GET("api/v1/riwayat/attempts/{attempt}")
    suspend fun detail(
        @Path("attempt") attemptId: Long,
    ): HistoryDetail

    @Streaming
    @GET("api/v1/riwayat/export")
    suspend fun export(): ResponseBody
}

@Singleton
class ApiHistoryPagesRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : HistoryPagesRepository {
        private val api = retrofit.create(HistoryApi::class.java)

        override suspend fun history(
            filters: HistoryFilters,
            page: Int,
        ): Result<HistoryPage> =
            apiCall(json) {
                api.history(
                    q = filters.q.trim().takeIf { it.isNotEmpty() },
                    mode = filters.mode.takeIf { it != HistoryMode.All }?.query,
                    category = filters.category,
                    sort = filters.sort.takeIf { it != HistorySort.Newest }?.query,
                    page = page.takeIf { it > 1 },
                )
            }

        override suspend fun detail(attemptId: Long): Result<HistoryDetail> = apiCall(json) { api.detail(attemptId) }

        override suspend fun export(): Result<InputStream> = apiCall(json) { api.export().byteStream() }
    }
