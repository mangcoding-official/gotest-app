package com.gotest.app.feature.ruang.data

import android.content.Context
import com.gotest.app.feature.ruang.domain.ExamBlock
import com.gotest.app.feature.ruang.domain.ExamDetail
import com.gotest.app.feature.ruang.domain.ExamItem
import com.gotest.app.feature.ruang.domain.ExamResultItem
import com.gotest.app.feature.ruang.domain.ExamResultPage
import com.gotest.app.feature.ruang.domain.ExamSchedule
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.MyExamStatus
import com.gotest.app.feature.ruang.domain.ScheduleState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
internal data class FakeExamPaper(
    val title: String,
    val blocks: List<ExamBlock>,
    val items: List<ExamItem>,
)

@Serializable
internal data class FakeExamResultItems(
    val items: List<ExamResultItem>,
)

/** The schedules of the debug Ujian data; texts are the server's (RuangAssessmentController, AssessmentAttempts). */
internal class FakeExamSchedules {
    private val opened = Instant.now().minus(OPENED_MINUTES_AGO, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
    private val closes = opened.plus(OPEN_HOURS, ChronoUnit.HOURS)

    fun mitra(code: String): MitraRef? =
        when (code) {
            FakeRuangRepository.SMP -> MitraRef(FakeExamRepository.SMP_NAME, code)
            FakeRuangRepository.BIMBEL -> MitraRef("Bimbel Cerdas", code)
            else -> null
        }

    fun list(
        code: String,
        attempt: FakeAttempt?,
    ): List<ExamSchedule> =
        when (code) {
            FakeRuangRepository.SMP -> {
                listOf(
                    ExamSchedule(
                        id = OPEN_ID,
                        title = "PTS Matematika Kelas 8",
                        opensAt = opened.toString(),
                        closesAt = closes.toString(),
                        durationMinutes = DURATION,
                        state = ScheduleState.Running,
                        myStatus = attempt?.myStatus ?: MyExamStatus.NotStarted,
                        attemptId = attempt?.id,
                    ),
                    ExamSchedule(
                        42,
                        "Penilaian Harian IPA",
                        "2026-12-02T00:30:00+00:00",
                        "2026-12-02T03:00:00+00:00",
                        60,
                        ScheduleState.Scheduled,
                        MyExamStatus.NotStarted,
                    ),
                    ExamSchedule(
                        40,
                        "Ulangan Bahasa Indonesia",
                        "2026-11-10T00:30:00+00:00",
                        "2026-11-10T03:00:00+00:00",
                        60,
                        ScheduleState.Closed,
                        MyExamStatus.Submitted,
                        RELEASED_ATTEMPT,
                        RELEASED_GRADE,
                    ),
                    ExamSchedule(
                        39,
                        "Kuis Sejarah Indonesia",
                        "2026-11-14T00:30:00+00:00",
                        "2026-11-14T02:00:00+00:00",
                        45,
                        ScheduleState.Closed,
                        MyExamStatus.Submitted,
                        WAITING_ATTEMPT,
                    ),
                )
            }

            FakeRuangRepository.BIMBEL -> {
                listOf(
                    ExamSchedule(
                        30,
                        "Try Out Matematika Bimbel",
                        "2026-10-20T01:00:00+00:00",
                        "2026-10-20T03:00:00+00:00",
                        90,
                        ScheduleState.Closed,
                        MyExamStatus.Submitted,
                        OLD_ATTEMPT,
                        OLD_GRADE,
                    ),
                )
            }

            else -> {
                emptyList()
            }
        }

    fun detail(
        code: String,
        scheduleId: Long,
        attempt: FakeAttempt?,
    ): ExamDetail? {
        val mitra = mitra(code) ?: return null
        val schedule = list(code, attempt).firstOrNull { it.id == scheduleId } ?: return null
        val blocked =
            when {
                schedule.attemptId != null -> {
                    null
                }

                schedule.state == ScheduleState.Scheduled -> {
                    "not_open" to "Ujian belum dibuka. Dibuka 2 Desember 2026 pukul 07.30 WIB."
                }

                schedule.state == ScheduleState.Closed -> {
                    "closed" to "Ujian sudah ditutup."
                }

                else -> {
                    null
                }
            }
        return ExamDetail(
            organization = mitra,
            scheduleId = schedule.id,
            title = schedule.title,
            instructions =
                "<p>Kerjakan dengan jujur. Gunakan kertas coretan bila perlu. " +
                    "Untuk isian angka, tulis angkanya saja tanpa satuan.</p>",
            questionCount = if (schedule.id == OPEN_ID) OPEN_QUESTIONS else OTHER_QUESTIONS,
            durationMinutes = schedule.durationMinutes,
            opensAt = schedule.opensAt,
            closesAt = schedule.closesAt,
            availableMinutes = schedule.durationMinutes,
            state = schedule.state,
            rules = RULES,
            myStatus = schedule.myStatus,
            attemptId = schedule.attemptId,
            canStart = blocked == null && schedule.myStatus != MyExamStatus.Submitted,
            blockedReason = blocked?.second,
            blockedCode = blocked?.first,
        )
    }

    /** A Rencana Belajar quiz result: grade, key, and pembahasan right away (web Ruang/Exams/Result, `mode: quiz`). */
    fun quizResult(
        json: Json,
        context: Context,
    ): ExamResultPage {
        val items =
            json.decodeFromString(
                FakeExamResultItems.serializer(),
                context.assets
                    .open("fake/ujian-hasil.json")
                    .bufferedReader()
                    .use { it.readText() },
            )
        return ExamResultPage(
            organization = MitraRef(FakeExamRepository.SMP_NAME, FakeRuangRepository.SMP),
            title = "",
            mode = ExamWorkPage.MODE_QUIZ,
            released = true,
            grade = QUIZ_GRADE,
            showAnswerKey = true,
            items = items.items.map { it.copy(explanation = QUIZ_EXPLANATION) },
        )
    }

    fun result(
        code: String,
        attemptId: Long,
        json: Json,
        context: Context,
    ): ExamResultPage? {
        val mitra = mitra(code) ?: return null
        return when (attemptId) {
            RELEASED_ATTEMPT -> {
                val items =
                    json.decodeFromString(
                        FakeExamResultItems.serializer(),
                        context.assets
                            .open("fake/ujian-hasil.json")
                            .bufferedReader()
                            .use { it.readText() },
                    )
                ExamResultPage(
                    organization = mitra,
                    title = "Ulangan Bahasa Indonesia",
                    submittedAt = "2026-11-10T00:41:00+00:00",
                    submitReason = "manual",
                    released = true,
                    grade = RELEASED_GRADE,
                    showAnswerKey = true,
                    items = items.items,
                )
            }

            WAITING_ATTEMPT -> {
                ExamResultPage(
                    organization = mitra,
                    title = "Kuis Sejarah Indonesia",
                    submittedAt = "2026-11-14T01:15:00+00:00",
                    submitReason = "time_up",
                    message = FakeExamRepository.NOT_RELEASED,
                )
            }

            OLD_ATTEMPT -> {
                ExamResultPage(
                    organization = mitra,
                    title = "Try Out Matematika Bimbel",
                    submittedAt = "2026-10-20T02:31:00+00:00",
                    submitReason = "violation",
                    released = true,
                    grade = OLD_GRADE,
                    items = (1..OTHER_QUESTIONS).map { ExamResultItem(it, if (it % 4 == 0) "wrong" else "correct") },
                )
            }

            else -> {
                null
            }
        }
    }

    private companion object {
        const val OPEN_ID = 41L
        const val RELEASED_ATTEMPT = 7L
        const val WAITING_ATTEMPT = 6L
        const val OLD_ATTEMPT = 5L
        const val RELEASED_GRADE = 82.5
        const val OLD_GRADE = 76.0
        const val DURATION = 90
        const val OPEN_QUESTIONS = 6
        const val OTHER_QUESTIONS = 20
        const val OPENED_MINUTES_AGO = 30L
        const val OPEN_HOURS = 3L
        const val QUIZ_GRADE = 80.0
        const val QUIZ_EXPLANATION =
            "<p>Baca lagi soalnya, lalu bandingkan jawabanmu dengan kunci. Pembahasan ini ditulis guru untuk kuis.</p>"

        val RULES =
            listOf(
                "Soal dan pilihan jawaban diacak untuk setiap siswa.",
                "Jawaban tersimpan otomatis. Waktu terus berjalan walau kamu keluar dari halaman.",
                "Keluar dari halaman Ujian 2 detik atau lebih dihitung sebagai pelanggaran dan terlihat oleh guru. " +
                    "Pada pelanggaran ke-3, jawabanmu dikumpulkan otomatis.",
                "Ujian hanya bisa dikerjakan di satu perangkat pada satu waktu.",
            )
    }
}
