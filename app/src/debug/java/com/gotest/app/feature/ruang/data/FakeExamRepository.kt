package com.gotest.app.feature.ruang.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.domain.ExamAnswerUpload
import com.gotest.app.feature.ruang.domain.ExamAnswersSaved
import com.gotest.app.feature.ruang.domain.ExamAttemptInfo
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamListPage
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.feature.ruang.domain.ExamStart
import com.gotest.app.feature.ruang.domain.ExamViolationRequest
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.MyExamStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug data for Ujian (debug builds only). In SMP 1 Bantargedung: "PTS Matematika Kelas 8" is open
 * (six items of all five types, from real bank questions, in `assets/fake/ujian-paper.json`), one Ujian
 * opens later, one has its grade shown with the key, and one waits for the teacher. Bimbel Cerdas has
 * one old result. Every call fails like a dropped connection while the phone has no network
 * (airplane mode), so the offline queue can be tried on the emulator. Messages are the server's.
 */
@Singleton
class FakeExamRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val json: Json,
    ) : ExamRepository {
        private val schedules = FakeExamSchedules()
        private val paper by lazy {
            json.decodeFromString(
                FakeExamPaper.serializer(),
                context.assets
                    .open("fake/ujian-paper.json")
                    .bufferedReader()
                    .use { it.readText() },
            )
        }
        private var attempt: FakeAttempt? = null

        override suspend fun schedules(code: String): Result<ExamListPage> =
            call {
                val mitra = schedules.mitra(code) ?: return@call notFound()
                Result.success(ExamListPage(mitra, schedules.list(code, attempt)))
            }

        override suspend fun detail(
            code: String,
            scheduleId: Long,
        ): Result<ExamDetail> =
            call {
                schedules.detail(code, scheduleId, attempt)?.let { Result.success(it) } ?: notFound()
            }

        override suspend fun start(
            code: String,
            scheduleId: Long,
        ): Result<ExamStart> =
            call {
                val detail = schedules.detail(code, scheduleId, attempt) ?: return@call notFound()
                val running = attempt?.takeIf { it.scheduleId == scheduleId }
                when {
                    running != null && running.submitted -> {
                        refuse(UNPROCESSABLE, "submitted", "Ujian ini sudah kamu kumpulkan.")
                    }

                    running != null -> {
                        Result.success(ExamStart(running.id, null, STATUS_RUNNING))
                    }

                    detail.blockedReason != null -> {
                        refuse(UNPROCESSABLE, detail.blockedCode ?: "not_ready", detail.blockedReason)
                    }

                    else -> {
                        val fresh =
                            FakeAttempt(scheduleId = scheduleId, deadline = Instant.now().plusSeconds(DURATION_SECONDS))
                        attempt = fresh
                        Result.success(ExamStart(fresh.id, fresh.token, STATUS_RUNNING))
                    }
                }
            }

        override suspend fun work(
            code: String,
            attemptId: Long,
            deviceToken: String?,
        ): Result<ExamWorkPage> =
            call {
                val current = attempt?.takeIf { it.id == attemptId }?.apply { closeIfLate() } ?: return@call notFound()
                if (current.submitted) return@call refuse(UNPROCESSABLE, "submitted", "Ujian ini sudah dikumpulkan.")
                Result.success(
                    ExamWorkPage(
                        organization = MitraRef(SMP_NAME, FakeRuangRepository.SMP),
                        title = paper.title,
                        attempt =
                            ExamAttemptInfo(
                                id = current.id,
                                status = STATUS_RUNNING,
                                deadlineAt = current.deadline.toString(),
                                serverNow = Instant.now().toString(),
                                violations = current.violations,
                                violationLimit = VIOLATION_LIMIT,
                                deviceToken = deviceToken?.takeIf { it == current.token },
                                lastClientSeq = current.lastSeq,
                            ),
                        blocks = paper.blocks,
                        items = paper.items,
                        answers = current.answers.mapKeys { it.key.toString() },
                    ),
                )
            }

        override suspend fun saveAnswers(
            attemptId: Long,
            deviceToken: String?,
            items: List<ExamAnswerUpload>,
        ): Result<ExamAnswersSaved> =
            call {
                val current = guard(attemptId, deviceToken).getOrElse { return@call Result.failure(it) }
                val saved = mutableListOf<Long>()
                val stale = mutableListOf<Long>()
                val rejected = mutableListOf<Long>()
                val now = Instant.now()
                items.forEach { item ->
                    when {
                        item.clientSeq <= (current.seqByItem[item.itemId] ?: 0) -> {
                            stale += item.itemId
                        }

                        now.isAfter(current.deadline.plusSeconds(GRACE_SECONDS)) ||
                            parseServerTime(item.answeredAt).isAfter(current.deadline) -> {
                            rejected += item.itemId
                        }

                        else -> {
                            if (item.answer is JsonNull) {
                                current.answers.remove(item.itemId)
                            } else {
                                current.answers[item.itemId] =
                                    item.answer
                            }
                            current.seqByItem[item.itemId] = item.clientSeq
                            saved += item.itemId
                        }
                    }
                    current.lastSeq = maxOf(current.lastSeq, item.clientSeq)
                }
                current.closeIfLate()
                Result.success(
                    ExamAnswersSaved(
                        saved = saved,
                        stale = stale,
                        rejected = rejected,
                        lastClientSeq = current.lastSeq,
                        serverNow = now.toString(),
                        deadlineAt = current.deadline.toString(),
                        status = if (current.submitted) STATUS_SUBMITTED else STATUS_RUNNING,
                    ),
                )
            }

        override suspend fun reportViolation(
            attemptId: Long,
            deviceToken: String?,
            violation: ExamViolationRequest,
        ): Result<ExamViolationResult> =
            call {
                val current = guard(attemptId, deviceToken).getOrElse { return@call Result.failure(it) }
                if (violation.awayMs >= MIN_AWAY_MS) current.violations++
                if (current.violations >= VIOLATION_LIMIT) current.submit("violation")
                Result.success(
                    ExamViolationResult(
                        violations = current.violations,
                        limit = VIOLATION_LIMIT,
                        status = if (current.submitted) STATUS_SUBMITTED else STATUS_RUNNING,
                    ),
                )
            }

        override suspend fun takeOver(attemptId: Long): Result<String> =
            call {
                val current = attempt?.takeIf { it.id == attemptId && !it.submitted } ?: return@call notFound()
                current.token = "fake-device-${System.nanoTime()}"
                Result.success(current.token)
            }

        override suspend fun submit(
            attemptId: Long,
            deviceToken: String?,
        ): Result<Unit> =
            call {
                val current = attempt?.takeIf { it.id == attemptId } ?: return@call notFound()
                if (!current.submitted) {
                    if (deviceToken != current.token) return@call otherDevice()
                    current.submit(if (Instant.now().isAfter(current.deadline)) "time_up" else "manual")
                }
                Result.success(Unit)
            }

        override suspend fun result(
            code: String,
            attemptId: Long,
        ): Result<ExamResultPage> =
            call {
                val current = attempt?.takeIf { it.id == attemptId }?.apply { closeIfLate() }
                when {
                    current != null && !current.submitted -> {
                        refuse(CONFLICT, "in_progress", "Ujian ini belum dikumpulkan.")
                    }

                    current != null -> {
                        Result.success(
                            ExamResultPage(
                                organization = MitraRef(SMP_NAME, FakeRuangRepository.SMP),
                                title = paper.title,
                                submittedAt = current.submittedAt?.toString(),
                                submitReason = current.reason,
                                message = NOT_RELEASED,
                            ),
                        )
                    }

                    else -> {
                        schedules.result(code, attemptId, json, context)?.let { Result.success(it) } ?: notFound()
                    }
                }
            }

        /** The same checks as the server before any write: this phone's token, and not submitted yet. */
        private fun guard(
            attemptId: Long,
            deviceToken: String?,
        ): Result<FakeAttempt> {
            val current = attempt?.takeIf { it.id == attemptId }?.apply { closeIfLate() } ?: return notFound()
            return when {
                current.submitted -> refuse(UNPROCESSABLE, "submitted", "Ujian ini sudah dikumpulkan.")
                deviceToken != current.token -> otherDevice()
                else -> Result.success(current)
            }
        }

        private suspend fun <T> call(block: () -> Result<T>): Result<T> {
            simulateLatency()
            if (!online()) return Result.failure(IOException("No network"))
            return block()
        }

        private fun online(): Boolean {
            val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        private fun <T> otherDevice(): Result<T> =
            refuse(CONFLICT, "other_device", "Ujian ini sedang dibuka di perangkat lain.")

        companion object {
            const val SMP_NAME = "SMP 1 Bantargedung"
            const val NOT_RELEASED = "Nilai tampil setelah diperiksa guru."
            private const val STATUS_RUNNING = "in_progress"
            private const val STATUS_SUBMITTED = "submitted"
            private const val VIOLATION_LIMIT = 3
            private const val MIN_AWAY_MS = 2_000L
            private const val GRACE_SECONDS = 120L
            private const val DURATION_SECONDS = 90L * 60
            private const val UNPROCESSABLE = 422
            private const val CONFLICT = 409
            private const val NOT_FOUND = 404

            fun <T> refuse(
                status: Int,
                code: String,
                message: String,
            ): Result<T> = Result.failure(ApiError(status, message, code))

            fun <T> notFound(): Result<T> = refuse(NOT_FOUND, "not_found", "Data tidak ditemukan.")
        }
    }

/** The one attempt the fake keeps (attempt 9 of "PTS Matematika Kelas 8"). */
internal class FakeAttempt(
    val scheduleId: Long,
    var deadline: Instant,
) {
    val id: Long = 9
    var token: String = "fake-device-start"
    var violations: Int = 0
    var lastSeq: Long = 0
    val answers = mutableMapOf<Long, kotlinx.serialization.json.JsonElement>()
    val seqByItem = mutableMapOf<Long, Long>()
    var submittedAt: Instant? = null
    var reason: String? = null
    val submitted: Boolean get() = submittedAt != null
    val myStatus: MyExamStatus get() = if (submitted) MyExamStatus.Submitted else MyExamStatus.InProgress

    fun submit(why: String) {
        if (!submitted) {
            submittedAt = Instant.now()
            reason = why
        }
    }

    /** The server submits an attempt whose time ran out (with the 2-minute grace for late answers). */
    fun closeIfLate() {
        if (Instant.now().isAfter(deadline.plusSeconds(GRACE))) submit("time_up")
    }

    private companion object {
        const val GRACE = 120L
    }
}
