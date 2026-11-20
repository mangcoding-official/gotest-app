package com.gotest.app.feature.tryout.data

import com.gotest.app.feature.practice.domain.AnswerRequest
import com.gotest.app.feature.tryout.domain.FinishSectionRequest
import com.gotest.app.feature.tryout.domain.FlagRequest
import com.gotest.app.feature.tryout.domain.SessionLeaderboard
import com.gotest.app.feature.tryout.domain.SessionMessage
import com.gotest.app.feature.tryout.domain.TryoutAttemptEnvelope
import com.gotest.app.feature.tryout.domain.TryoutCatalogPage
import com.gotest.app.feature.tryout.domain.TryoutResultEnvelope
import com.gotest.app.feature.tryout.domain.TryoutSessionPage
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** API module D routes (API-CONTRACTS "API Android modul D"); the web's address plus `/api/v1`. */
@Suppress("TooManyFunctions") // One function per route of the contract.
interface TryoutApi {
    @GET("api/v1/tryout/packages")
    suspend fun catalog(): TryoutCatalogPage

    @POST("api/v1/tryout/packages/{examPackage}/attempts")
    suspend fun start(
        @Path("examPackage") packageId: Long,
    ): TryoutAttemptEnvelope

    @GET("api/v1/tryout/attempts/{attempt}")
    suspend fun attempt(
        @Path("attempt") attemptId: Long,
    ): TryoutAttemptEnvelope

    @POST("api/v1/tryout/attempts/{attempt}/begin")
    suspend fun begin(
        @Path("attempt") attemptId: Long,
    ): TryoutAttemptEnvelope

    @PATCH("api/v1/tryout/attempts/{attempt}/answers/{item}")
    suspend fun answer(
        @Path("attempt") attemptId: Long,
        @Path("item") itemId: Long,
        @Body body: AnswerRequest,
    )

    @PATCH("api/v1/tryout/attempts/{attempt}/questions/{item}/flag")
    suspend fun flag(
        @Path("attempt") attemptId: Long,
        @Path("item") itemId: Long,
        @Body body: FlagRequest,
    )

    @POST("api/v1/tryout/attempts/{attempt}/finish-section")
    suspend fun finishSection(
        @Path("attempt") attemptId: Long,
        @Body body: FinishSectionRequest,
    ): TryoutAttemptEnvelope

    @POST("api/v1/tryout/attempts/{attempt}/submit")
    suspend fun submit(
        @Path("attempt") attemptId: Long,
    ): TryoutAttemptEnvelope

    @GET("api/v1/tryout/attempts/{attempt}/result")
    suspend fun result(
        @Path("attempt") attemptId: Long,
    ): TryoutResultEnvelope

    @GET("api/v1/tryout/sesi/{session}")
    suspend fun session(
        @Path("session") sessionId: Long,
    ): TryoutSessionPage

    @POST("api/v1/tryout/sesi/{session}/daftar")
    suspend fun register(
        @Path("session") sessionId: Long,
    ): SessionMessage

    @DELETE("api/v1/tryout/sesi/{session}/daftar")
    suspend fun unregister(
        @Path("session") sessionId: Long,
    ): SessionMessage

    @POST("api/v1/tryout/sesi/{session}/attempts")
    suspend fun startSession(
        @Path("session") sessionId: Long,
    ): TryoutAttemptEnvelope

    @GET("api/v1/tryout/sesi/{session}/leaderboard")
    suspend fun sessionLeaderboard(
        @Path("session") sessionId: Long,
    ): SessionLeaderboard
}
