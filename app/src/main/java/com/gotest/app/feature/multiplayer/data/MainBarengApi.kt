package com.gotest.app.feature.multiplayer.data

import com.gotest.app.feature.multiplayer.domain.ActiveGameEnvelope
import com.gotest.app.feature.multiplayer.domain.GameEnvelope
import com.gotest.app.feature.multiplayer.domain.GameReview
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.HistoryEnvelope
import com.gotest.app.feature.multiplayer.domain.HubPage
import com.gotest.app.feature.multiplayer.domain.QuestionsEnvelope
import com.gotest.app.feature.practice.domain.AnswerRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** API module E routes (API-CONTRACTS "API Android modul E"); the web's address plus `/api/v1`. */
@Suppress("TooManyFunctions") // One function per route of the contract.
interface MainBarengApi {
    @GET("api/v1/main-bareng")
    suspend fun hub(): HubPage

    @GET("api/v1/ruang/{code}/main-bareng")
    suspend fun ruangHub(
        @Path("code") space: String,
    ): HubPage

    @GET("api/v1/main-bareng/aktif")
    suspend fun active(): ActiveGameEnvelope

    @GET("api/v1/main-bareng/riwayat")
    suspend fun history(): HistoryEnvelope

    @GET("api/v1/ruang/{code}/main-bareng/riwayat")
    suspend fun ruangHistory(
        @Path("code") space: String,
    ): HistoryEnvelope

    @POST("api/v1/main-bareng")
    suspend fun create(
        @Body settings: GameSettings,
    ): GameEnvelope

    @POST("api/v1/ruang/{code}/main-bareng")
    suspend fun ruangCreate(
        @Path("code") space: String,
        @Body settings: GameSettings,
    ): GameEnvelope

    @GET("api/v1/main-bareng/kode/{code}")
    suspend fun lookup(
        @Path("code") code: String,
    ): GameEnvelope

    @POST("api/v1/main-bareng/kode/{code}/gabung")
    suspend fun join(
        @Path("code") code: String,
    ): GameEnvelope

    @GET("api/v1/main-bareng/{code}")
    suspend fun state(
        @Path("code") code: String,
    ): GameEnvelope

    @POST("api/v1/main-bareng/{code}/mulai")
    suspend fun start(
        @Path("code") code: String,
    ): GameEnvelope

    @POST("api/v1/main-bareng/{code}/batal")
    suspend fun cancel(
        @Path("code") code: String,
    ): GameEnvelope

    @POST("api/v1/main-bareng/{code}/keluar")
    suspend fun leave(
        @Path("code") code: String,
    )

    @GET("api/v1/main-bareng/{code}/soal")
    suspend fun questions(
        @Path("code") code: String,
    ): QuestionsEnvelope

    @PUT("api/v1/main-bareng/{code}/jawaban/{item}")
    suspend fun answer(
        @Path("code") code: String,
        @Path("item") itemId: Long,
        @Body body: AnswerRequest,
    )

    @POST("api/v1/main-bareng/{code}/selesai")
    suspend fun finish(
        @Path("code") code: String,
    ): GameEnvelope

    @GET("api/v1/main-bareng/{code}/hasil")
    suspend fun result(
        @Path("code") code: String,
    ): GameReview

    @POST("api/v1/main-bareng/{code}/main-lagi")
    suspend fun playAgain(
        @Path("code") code: String,
    ): GameEnvelope
}
