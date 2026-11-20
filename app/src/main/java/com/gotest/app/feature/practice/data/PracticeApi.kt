package com.gotest.app.feature.practice.data

import com.gotest.app.feature.practice.domain.ActivatedQuestion
import com.gotest.app.feature.practice.domain.AnswerRequest
import com.gotest.app.feature.practice.domain.AttemptEnvelope
import com.gotest.app.feature.practice.domain.PracticeCatalogPage
import com.gotest.app.feature.practice.domain.ResultEnvelope
import com.gotest.app.feature.practice.domain.StartAccess
import com.gotest.app.feature.practice.domain.StartRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** API module C routes for Latihan (API-CONTRACTS "API Android modul B dan C"); the web's address plus `/api/v1`. */
interface PracticeApi {
    @GET("api/v1/practice/packages")
    suspend fun catalog(): PracticeCatalogPage

    @GET("api/v1/akun")
    suspend fun account(): StartAccess

    @POST("api/v1/packages/{examPackage}/attempts")
    suspend fun start(
        @Path("examPackage") packageId: Long,
        @Body body: StartRequest,
    ): AttemptEnvelope

    @GET("api/v1/attempts/{attempt}")
    suspend fun attempt(
        @Path("attempt") attemptId: Long,
    ): AttemptEnvelope

    @POST("api/v1/attempts/{attempt}/questions/{item}/activate")
    suspend fun activate(
        @Path("attempt") attemptId: Long,
        @Path("item") itemId: Long,
    ): ActivatedQuestion

    @PATCH("api/v1/attempts/{attempt}/answers/{question}")
    suspend fun answer(
        @Path("attempt") attemptId: Long,
        @Path("question") questionId: Long,
        @Body body: AnswerRequest,
    )

    @POST("api/v1/attempts/{attempt}/submit")
    suspend fun submit(
        @Path("attempt") attemptId: Long,
    ): AttemptEnvelope

    @GET("api/v1/attempts/{attempt}/result")
    suspend fun result(
        @Path("attempt") attemptId: Long,
    ): ResultEnvelope
}
