package com.gotest.app.feature.ruang.data

import com.gotest.app.feature.ruang.domain.ExamAnswersRequest
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamListPage
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.feature.ruang.domain.ExamStart
import com.gotest.app.feature.ruang.domain.ExamSubmitted
import com.gotest.app.feature.ruang.domain.ExamTakeOver
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.feature.ruang.domain.InvitationCodeSent
import com.gotest.app.feature.ruang.domain.InvitationPage
import com.gotest.app.feature.ruang.domain.JoinCodeLookup
import com.gotest.app.feature.ruang.domain.JoinCodePage
import com.gotest.app.feature.ruang.domain.JoinResult
import com.gotest.app.feature.ruang.domain.MitraMembership
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.StudyPlanPage
import com.gotest.app.feature.ruang.domain.TaskMessage
import com.gotest.app.feature.ruang.domain.TaskRecord
import com.gotest.app.feature.ruang.domain.TaskStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** The part of `GET /akun` this module reads. */
@Serializable
data class AccountOrganizations(
    @SerialName("organizations") val organizations: List<MitraMembership> = emptyList(),
    @SerialName("token") val token: AccountToken? = null,
)

/** This phone's token; [expiresAt] is when this sign-in ends (API-K4). */
@Serializable
data class AccountToken(
    @SerialName("name") val name: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class InvitationAcceptRequest(
    @SerialName("code") val code: String? = null,
)

/**
 * API module G routes (API-CONTRACTS "API Android modul G") and the child's Rencana Belajar (T-031); the address
 * is the web's plus `/api/v1`.
 */
@Suppress("TooManyFunctions") // One function per route of the contract.
interface RuangApi {
    @GET("api/v1/akun")
    suspend fun account(): AccountOrganizations

    @GET("api/v1/ruang/{code}")
    suspend fun ruang(
        @Path("code") code: String,
    ): RuangPage

    @POST("api/v1/gabung")
    suspend fun lookupCode(
        @Body body: JoinCodeLookup,
    ): JoinCodeLookup

    @GET("api/v1/gabung/{code}")
    suspend fun joinCode(
        @Path("code") code: String,
    ): JoinCodePage

    @POST("api/v1/gabung/{code}")
    suspend fun join(
        @Path("code") code: String,
    ): JoinResult

    @GET("api/v1/undangan/{token}")
    suspend fun invitation(
        @Path("token") token: String,
    ): InvitationPage

    @POST("api/v1/undangan/{token}/kode")
    suspend fun sendInvitationCode(
        @Path("token") token: String,
    ): InvitationCodeSent

    @POST("api/v1/undangan/{token}")
    suspend fun acceptInvitation(
        @Path("token") token: String,
        @Body body: InvitationAcceptRequest,
    ): JoinResult

    @GET("api/v1/ruang/{code}/ujian")
    suspend fun schedules(
        @Path("code") code: String,
    ): ExamListPage

    @GET("api/v1/ruang/{code}/ujian/{schedule}")
    suspend fun detail(
        @Path("code") code: String,
        @Path("schedule") scheduleId: Long,
    ): ExamDetail

    @POST("api/v1/ruang/{code}/ujian/{schedule}/mulai")
    suspend fun start(
        @Path("code") code: String,
        @Path("schedule") scheduleId: Long,
    ): ExamStart

    @GET("api/v1/ruang/{code}/ujian/kerjakan/{attempt}")
    suspend fun work(
        @Path("code") code: String,
        @Path("attempt") attemptId: Long,
        @Header(EXAM_DEVICE) deviceToken: String?,
    ): ExamWorkPage

    @PUT("api/v1/ujian/pengerjaan/{attempt}/jawaban")
    suspend fun saveAnswers(
        @Path("attempt") attemptId: Long,
        @Header(EXAM_DEVICE) deviceToken: String?,
        @Body body: ExamAnswersRequest,
    ): ExamAnswersSaved

    @POST("api/v1/ujian/pengerjaan/{attempt}/pelanggaran")
    suspend fun reportViolation(
        @Path("attempt") attemptId: Long,
        @Header(EXAM_DEVICE) deviceToken: String?,
        @Body body: ExamViolationRequest,
    ): ExamViolationResult

    @POST("api/v1/ujian/pengerjaan/{attempt}/ambil-alih")
    suspend fun takeOver(
        @Path("attempt") attemptId: Long,
    ): ExamTakeOver

    @POST("api/v1/ujian/pengerjaan/{attempt}/kumpulkan")
    suspend fun submit(
        @Path("attempt") attemptId: Long,
        @Header(EXAM_DEVICE) deviceToken: String?,
    ): ExamSubmitted

    @GET("api/v1/ruang/{code}/ujian/hasil/{attempt}")
    suspend fun result(
        @Path("code") code: String,
        @Path("attempt") attemptId: Long,
    ): ExamResultPage

    @GET("api/v1/ruang/{code}/kuis/kerjakan/{attempt}")
    suspend fun quizWork(
        @Path("code") code: String,
        @Path("attempt") attemptId: Long,
        @Header(EXAM_DEVICE) deviceToken: String?,
    ): ExamWorkPage

    @GET("api/v1/ruang/{code}/kuis/hasil/{attempt}")
    suspend fun quizResult(
        @Path("code") code: String,
        @Path("attempt") attemptId: Long,
    ): ExamResultPage

    @GET("api/v1/ruang/{code}/rencana")
    suspend fun plan(
        @Path("code") code: String,
    ): StudyPlanPage

    @POST("api/v1/ruang/{code}/tugas/{task}/mulai")
    suspend fun startTask(
        @Path("code") code: String,
        @Path("task") taskId: Long,
    ): TaskStart

    @PUT("api/v1/ruang/{code}/tugas/{task}/catatan")
    suspend fun recordTask(
        @Path("code") code: String,
        @Path("task") taskId: Long,
        @Body body: TaskRecord,
    ): TaskMessage

    @DELETE("api/v1/ruang/{code}/tugas/{task}/catatan")
    suspend fun unrecordTask(
        @Path("code") code: String,
        @Path("task") taskId: Long,
    ): TaskMessage

    companion object {
        /** The Ujian device token, kept per attempt apart from the sign-in token. */
        const val EXAM_DEVICE = "X-Exam-Device"
    }
}
