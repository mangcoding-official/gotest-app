package com.gotest.app.feature.ruang.data

import com.gotest.app.core.network.apiCall
import com.gotest.app.feature.ruang.domain.ExamAnswerUpload
import com.gotest.app.feature.ruang.domain.ExamAnswersRequest
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamListPage
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.feature.ruang.domain.ExamStart
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.feature.ruang.domain.InvitationPage
import com.gotest.app.feature.ruang.domain.JoinCodeLookup
import com.gotest.app.feature.ruang.domain.JoinCodePage
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.domain.MitraMembership
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangRepository
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiRuangRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : RuangRepository {
        private val api = retrofit.create(RuangApi::class.java)

        override suspend fun memberships(): Result<List<MitraMembership>> =
            apiCall(json) { api.account().organizations }

        override suspend fun ruang(code: String): Result<RuangPage> = apiCall(json) { api.ruang(code) }

        override suspend fun lookupCode(typed: String): Result<String> =
            apiCall(json) { api.lookupCode(JoinCodeLookup(typed)).code }

        override suspend fun joinCode(code: String): Result<JoinCodePage> = apiCall(json) { api.joinCode(code) }

        override suspend fun join(code: String): Result<JoinedMitra> = apiCall(json) { api.join(code).joined }

        override suspend fun invitation(token: String): Result<InvitationPage> = apiCall(json) { api.invitation(token) }

        override suspend fun sendInvitationCode(token: String): Result<String?> =
            apiCall(json) { api.sendInvitationCode(token).emailCodeSentTo }

        override suspend fun acceptInvitation(
            token: String,
            code: String?,
        ): Result<JoinedMitra> = apiCall(json) { api.acceptInvitation(token, InvitationAcceptRequest(code)).joined }
    }

@Singleton
class ApiExamRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
    ) : ExamRepository {
        private val api = retrofit.create(RuangApi::class.java)

        override suspend fun schedules(code: String): Result<ExamListPage> = apiCall(json) { api.schedules(code) }

        override suspend fun detail(
            code: String,
            scheduleId: Long,
        ): Result<ExamDetail> = apiCall(json) { api.detail(code, scheduleId) }

        override suspend fun start(
            code: String,
            scheduleId: Long,
        ): Result<ExamStart> = apiCall(json) { api.start(code, scheduleId) }

        override suspend fun work(
            code: String,
            attemptId: Long,
            deviceToken: String?,
        ): Result<ExamWorkPage> = apiCall(json) { api.work(code, attemptId, deviceToken) }

        override suspend fun saveAnswers(
            attemptId: Long,
            deviceToken: String?,
            items: List<ExamAnswerUpload>,
        ): Result<ExamAnswersSaved> =
            apiCall(json) { api.saveAnswers(attemptId, deviceToken, ExamAnswersRequest(items)) }

        override suspend fun reportViolation(
            attemptId: Long,
            deviceToken: String?,
            violation: ExamViolationRequest,
        ): Result<ExamViolationResult> = apiCall(json) { api.reportViolation(attemptId, deviceToken, violation) }

        override suspend fun takeOver(attemptId: Long): Result<String> =
            apiCall(json) { api.takeOver(attemptId).deviceToken }

        override suspend fun submit(
            attemptId: Long,
            deviceToken: String?,
        ): Result<Unit> = apiCall(json) { api.submit(attemptId, deviceToken) }.map { }

        override suspend fun result(
            code: String,
            attemptId: Long,
        ): Result<ExamResultPage> = apiCall(json) { api.result(code, attemptId) }
    }
