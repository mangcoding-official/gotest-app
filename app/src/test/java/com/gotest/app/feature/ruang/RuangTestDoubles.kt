package com.gotest.app.feature.ruang

import com.gotest.app.core.network.ApiError
import com.gotest.app.core.session.KeyValueStore
import com.gotest.app.core.session.TokenCipher
import com.gotest.app.feature.ruang.data.ExamLocalStore
import com.gotest.app.feature.ruang.domain.ExamAnswerUpload
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamListPage
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.feature.ruang.domain.ExamStart
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import kotlinx.serialization.json.Json
import java.io.IOException

internal class MemoryKeyValueStore : KeyValueStore {
    val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

/** Reverses the text; [failing] makes it throw like a broken Keystore. */
internal class ReversingCipher(
    var failing: Boolean = false,
) : TokenCipher {
    override fun encrypt(plain: String): String {
        if (failing) throw java.security.ProviderException("Keystore unavailable")
        return plain.reversed()
    }

    override fun decrypt(encrypted: String): String? {
        if (failing) throw java.security.ProviderException("Keystore unavailable")
        return encrypted.reversed()
    }
}

internal fun testLocalStore(cipher: TokenCipher = ReversingCipher()) =
    ExamLocalStore(MemoryKeyValueStore(), cipher, Json { ignoreUnknownKeys = true })

internal val offline: Result<Nothing> get() = Result.failure(IOException("offline"))

internal fun refused(
    status: Int,
    code: String,
    message: String = code,
): Result<Nothing> = Result.failure(ApiError(status, message, code))

/** Answers what the test sets; every call is recorded. */
internal class ScriptedExamRepository : ExamRepository {
    var workPage: Result<ExamWorkPage> = offline
    val saveReplies = ArrayDeque<Result<ExamAnswersSaved>>()
    val violationReplies = ArrayDeque<Result<ExamViolationResult>>()
    val savedBatches = mutableListOf<List<ExamAnswerUpload>>()
    val reportedViolations = mutableListOf<ExamViolationRequest>()
    var submitCalls = 0

    /** Runs while a report is on its way, before the reply. */
    var whileReporting: (() -> Unit)? = null

    override suspend fun schedules(code: String): Result<ExamListPage> = offline

    override suspend fun detail(
        code: String,
        scheduleId: Long,
    ): Result<ExamDetail> = offline

    override suspend fun start(
        code: String,
        scheduleId: Long,
    ): Result<ExamStart> = offline

    override suspend fun work(
        code: String,
        attemptId: Long,
        deviceToken: String?,
    ): Result<ExamWorkPage> = workPage

    override suspend fun saveAnswers(
        attemptId: Long,
        deviceToken: String?,
        items: List<ExamAnswerUpload>,
    ): Result<ExamAnswersSaved> {
        savedBatches += items
        return saveReplies.removeFirstOrNull() ?: offline
    }

    override suspend fun reportViolation(
        attemptId: Long,
        deviceToken: String?,
        violation: ExamViolationRequest,
    ): Result<ExamViolationResult> {
        reportedViolations += violation
        whileReporting?.invoke()
        return violationReplies.removeFirstOrNull() ?: offline
    }

    override suspend fun takeOver(attemptId: Long): Result<String> = offline

    override suspend fun submit(
        attemptId: Long,
        deviceToken: String?,
    ): Result<Unit> {
        submitCalls++
        return Result.success(Unit)
    }

    override suspend fun result(
        code: String,
        attemptId: Long,
    ): Result<ExamResultPage> = offline
}
