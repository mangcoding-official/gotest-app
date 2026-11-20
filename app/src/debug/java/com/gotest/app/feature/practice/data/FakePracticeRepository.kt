package com.gotest.app.feature.practice.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.AccountAccess
import com.gotest.app.feature.practice.domain.ActiveQuestion
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.CatalogCategory
import com.gotest.app.feature.practice.domain.FreePlan
import com.gotest.app.feature.practice.domain.FreeQuota
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeCatalogPage
import com.gotest.app.feature.practice.domain.PracticeItem
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.practice.domain.PracticeRepository
import com.gotest.app.feature.practice.domain.PracticeResult
import com.gotest.app.feature.practice.domain.RecentAttempt
import com.gotest.app.feature.practice.domain.ReviewItem
import com.gotest.app.feature.practice.domain.StartAccess
import com.gotest.app.feature.practice.domain.StudentStats
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Debug data for Latihan (API module C; debug builds only, T-026 criterion 8). UTBK-SNBT has a package without a
 * timer, one with an optional total timer, one timed per question (answered in order), one that cannot be done
 * again (opens its pembahasan), and a Premium package locked for this Gratis account; TKA SMA has one done once.
 * Gilang is on Gratis with 2 of 3 Latihan left today. Every call fails like a dropped connection while the phone
 * has no network (airplane mode). Messages are the server's (PracticeAttemptController, PracticeAttemptService).
 */
@Singleton
class FakePracticeRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PracticeRepository {
        private val packages = FakePracticeData.packages().toMutableList()
        private val attempts = mutableMapOf<Long, FakePracticeAttempt>()
        private var nextId = FIRST_ATTEMPT
        private var practiceUsed = 1

        init {
            // The finished attempts behind the catalog's "Lihat Pembahasan" and Aktifitas.
            FakePracticeData.finished.forEach { (id, packageId) ->
                attempts[id] = FakePracticeAttempt(id, packageId, PracticeAttempt.TIMER_NONE, false, null).apply {
                    FakePracticeData.questions.forEachIndexed { index, question ->
                        if (index % 2 == 0) answers[question.id] = question.correct
                    }
                    status = "submitted"
                    submittedAt = Instant.parse("2026-11-14T09:30:00Z")
                }
            }
        }

        override suspend fun catalog(): Result<PracticeCatalogPage> =
            call {
                Result.success(
                    PracticeCatalogPage(
                        categories = FakePracticeData.categories,
                        packages = packages.map(::withProgress),
                        recentAttempts = FakePracticeData.recent,
                        stats = StudentStats(questionsDone = 480, accuracy = 68, packagesDone = 19),
                    ),
                )
            }

        override suspend fun startAccess(): Result<StartAccess> =
            call {
                Result.success(
                    StartAccess(
                        access = AccountAccess("active"),
                        freePlan = FreePlan(practice = FreeQuota(practiceUsed, FREE_PRACTICE), tryout = FreeQuota(0, 2)),
                    ),
                )
            }

        override suspend fun start(
            packageId: Long,
            timerEnabled: Boolean?,
        ): Result<PracticeAttempt> =
            call start@{
                val item = packages.firstOrNull { it.id == packageId } ?: return@start notFound()
                attempts.values.firstOrNull { it.packageId == packageId && it.running }?.let {
                    return@start Result.success(it.snapshot())
                }
                when {
                    item.premiumLocked -> {
                        refuse(FORBIDDEN, "premium_only", "Paket ini khusus Premium.")
                    }

                    !item.allowRetry && item.finishedCount > 0 -> {
                        validation("package", "Paket ini tidak dapat dikerjakan ulang.")
                    }

                    practiceUsed >= FREE_PRACTICE -> {
                        refuse(
                            FORBIDDEN,
                            "quota_used",
                            "Kuota Latihan gratis hari ini sudah habis. Coba lagi besok atau gunakan Premium.",
                        )
                    }

                    else -> {
                        practiceUsed++
                        val timer = FakePracticeData.timerOf(packageId)
                        val enabled = timer != PracticeAttempt.TIMER_NONE && timerEnabled != false
                        val deadline =
                            if (enabled && timer == PracticeAttempt.TIMER_PACKAGE) {
                                Instant.now().plusSeconds(PACKAGE_SECONDS)
                            } else {
                                null
                            }
                        val fresh = FakePracticeAttempt(nextId++, packageId, timer, enabled, deadline)
                        attempts[fresh.id] = fresh
                        Result.success(fresh.snapshot())
                    }
                }
            }

        override suspend fun attempt(attemptId: Long): Result<PracticeAttempt> =
            call { attempts[attemptId]?.let { Result.success(it.snapshot()) } ?: notFound() }

        override suspend fun activate(
            attemptId: Long,
            itemId: Long,
        ): Result<ActiveQuestion> =
            call activate@{
                val current = attempts[attemptId]?.apply { closeIfLate() } ?: return@activate notFound()
                if (!current.running) return@activate validation("attempt", FINISHED)
                current.activate(itemId).map { question ->
                    ActiveQuestion(itemId, question.id, current.orderOf(itemId), question.type, question.content)
                }
            }

        override suspend fun answer(
            attemptId: Long,
            questionId: Long,
            answer: AnswerData,
        ): Result<Unit> =
            call answer@{
                val current = attempts[attemptId]?.apply { closeIfLate() } ?: return@answer notFound()
                if (!current.running) return@answer validation("answer", FINISHED)
                current.answer(questionId, answer)
            }

        override suspend fun submit(attemptId: Long): Result<PracticeAttempt> =
            call submit@{
                val current = attempts[attemptId] ?: return@submit notFound()
                current.closeIfLate()
                if (current.running) current.finish("submitted")
                packages.replaceAll { if (it.id == current.packageId) finishedRow(it, current) else it }
                Result.success(current.snapshot())
            }

        override suspend fun result(attemptId: Long): Result<PracticeResult> =
            call result@{
                val current = attempts[attemptId]?.apply { closeIfLate() } ?: return@result notFound()
                if (current.running) return@result notFound()
                Result.success(current.result())
            }

        private fun withProgress(item: PracticePackage): PracticePackage {
            val running = attempts.values.firstOrNull { it.packageId == item.id && it.running } ?: return item
            val answered = running.answers.size
            return item.copy(
                continueAttemptId = running.id,
                answeredCount = answered,
                questionCount = FakePracticeData.questions.size,
                progress = answered * PERCENT / FakePracticeData.questions.size,
            )
        }

        private fun finishedRow(
            item: PracticePackage,
            done: FakePracticeAttempt,
        ) = item.copy(
            continueAttemptId = null,
            completed = true,
            finishedCount = item.finishedCount + 1,
            lastAttemptId = done.id,
            lastFinishedAt = done.submittedAt?.toString(),
            lastScore = done.score(),
            lastScoreLabel = "Nilai",
        )

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

        companion object {
            const val FINISHED = "Latihan ini sudah selesai sehingga jawaban tidak dapat diubah."
            private const val FIRST_ATTEMPT = 1001L
            private const val FREE_PRACTICE = 3
            private const val PACKAGE_SECONDS = 10L * 60
            private const val PERCENT = 100
            private const val FORBIDDEN = 403
            private const val NOT_FOUND = 404
            private const val UNPROCESSABLE = 422

            fun <T> refuse(
                status: Int,
                code: String,
                message: String,
            ): Result<T> = Result.failure(ApiError(status, message, code))

            fun <T> validation(
                field: String,
                message: String,
            ): Result<T> = Result.failure(ApiError(UNPROCESSABLE, message, "validation", mapOf(field to listOf(message))))

            fun <T> notFound(): Result<T> = refuse(NOT_FOUND, "not_found", "Data tidak ditemukan.")
        }
    }

/** One attempt of the fake: the server's timer and order rules, kept small. */
internal class FakePracticeAttempt(
    val id: Long,
    val packageId: Long,
    private val timerMode: String,
    private val timerEnabled: Boolean,
    private val deadline: Instant?,
) {
    private val questions = FakePracticeData.questions
    private val itemIds = questions.indices.map { id * ITEMS + it + 1 }
    val answers = mutableMapOf<Long, AnswerData>()
    private val opened = mutableSetOf<Long>()
    private val timedOut = mutableSetOf<Long>()
    private val skipped = mutableSetOf<Long>()
    private val questionDeadlines = mutableMapOf<Long, Instant>()
    private var activeItemId: Long? = null
    var status = PracticeAttempt.STATUS_STARTED
    var submittedAt: Instant? = null

    val running: Boolean get() = status == PracticeAttempt.STATUS_STARTED
    private val perQuestion: Boolean get() = timerEnabled && timerMode == PracticeAttempt.TIMER_PER_QUESTION

    fun orderOf(itemId: Long): Int = itemIds.indexOf(itemId) + 1

    fun finish(why: String) {
        if (running) {
            status = why
            submittedAt = Instant.now()
        }
    }

    /** The server closes a question whose time ran out, and the whole attempt when the package time ran out. */
    fun closeIfLate() {
        if (!running) return
        val now = Instant.now()
        if (deadline != null && now.isAfter(deadline)) finish("auto_submitted")
        val active = activeItemId ?: return
        val due = questionDeadlines[active] ?: return
        if (perQuestion && now.isAfter(due) && active !in timedOut) {
            timedOut += active
            val next = itemIds.getOrNull(itemIds.indexOf(active) + 1)
            if (next == null) finish("auto_submitted") else activeItemId = next
        }
    }

    @Suppress("ReturnCount")
    fun activate(itemId: Long): Result<FakeQuestion> {
        val index = itemIds.indexOf(itemId)
        if (index < 0) return FakePracticeRepository.notFound()
        if (perQuestion) {
            val activeIndex = activeItemId?.let(itemIds::indexOf) ?: 0
            when {
                itemId in timedOut -> {
                    return FakePracticeRepository.validation("question", "Soal ini telah dikunci karena waktu habis.")
                }

                itemId in skipped || index < activeIndex -> {
                    return FakePracticeRepository.validation(
                        "question",
                        "Soal ini sudah dilewati dan tidak dapat dibuka kembali.",
                    )
                }

                index > activeIndex + 1 -> {
                    return FakePracticeRepository.validation("question", "Kerjakan soal secara berurutan.")
                }

                index == activeIndex + 1 -> {
                    activeItemId?.let { if (it !in timedOut) skipped += it }
                }
            }
            if (itemId !in questionDeadlines) questionDeadlines[itemId] = Instant.now().plusSeconds(QUESTION_SECONDS)
        }
        activeItemId = itemId
        opened += itemId
        return Result.success(questions[index])
    }

    @Suppress("ReturnCount")
    fun answer(
        questionId: Long,
        answer: AnswerData,
    ): Result<Unit> {
        val index = questions.indexOfFirst { it.id == questionId }
        if (index < 0) return FakePracticeRepository.notFound()
        val itemId = itemIds[index]
        return when {
            itemId in timedOut -> FakePracticeRepository.validation("answer", "Waktu untuk soal ini telah habis.")
            itemId in skipped -> FakePracticeRepository.validation("answer", "Soal ini sudah dilewati sehingga jawabannya tidak dapat diubah.")
            itemId !in opened -> FakePracticeRepository.validation("answer", "Buka soal ini terlebih dahulu sebelum menjawab.")
            else -> Result.success(Unit).also { answers[questionId] = answer }
        }
    }

    fun snapshot(): PracticeAttempt {
        closeIfLate()
        return PracticeAttempt(
            id = id,
            status = status,
            serverNow = Instant.now().toString(),
            submittedAt = submittedAt?.toString(),
            timerMode = if (timerEnabled) timerMode else PracticeAttempt.TIMER_NONE,
            timerEnabled = timerEnabled,
            deadlineAt = deadline?.toString(),
            activeItemId = activeItemId,
            navigation = if (perQuestion) "linear" else "free",
            items =
                questions.mapIndexed { index, question ->
                    val itemId = itemIds[index]
                    val answer = answers[question.id]
                    PracticeItem(
                        id = itemId,
                        questionId = question.id,
                        displayOrder = index + 1,
                        answeredAt = answer?.let { Instant.now().toString() },
                        questionType = question.type,
                        subjectName = question.subject,
                        selectedOption = answer?.selectedOption,
                        selectedOptions = answer?.selectedOptions.orEmpty(),
                        statementAnswers = answer?.statementAnswers.orEmpty(),
                        questionDeadlineAt = questionDeadlines[itemId]?.toString(),
                        timedOut = itemId in timedOut,
                        locked = itemId in skipped || itemId in timedOut,
                    )
                },
        )
    }

    fun score(): Double = (correctCount() * PERCENT.toDouble() / questions.size * TENTHS).roundToInt() / TENTHS

    private fun correctCount(): Int = questions.count { answers[it.id] == it.correct }

    fun result(): PracticeResult {
        val correct = correctCount()
        val empty = questions.count { it.id !in answers }
        return PracticeResult(
            id = id,
            status = status,
            score = score(),
            correctCount = correct,
            wrongCount = questions.size - correct - empty,
            emptyCount = empty,
            submittedAt = submittedAt?.toString(),
            items =
                questions.mapIndexed { index, question ->
                    val answer = answers[question.id]
                    ReviewItem(
                        id = itemIds[index],
                        displayOrder = index + 1,
                        questionType = question.type,
                        selectedOption = answer?.selectedOption,
                        correctOption = question.correct.selectedOption,
                        selectedOptions = answer?.selectedOptions.orEmpty(),
                        correctOptions = question.correct.selectedOptions.orEmpty(),
                        statementAnswers = answer?.statementAnswers.orEmpty(),
                        statementCorrectAnswers = question.correct.statementAnswers.orEmpty(),
                        isCorrect = answer?.let { it == question.correct },
                        isEmpty = answer == null,
                        scoreAwarded = if (answer == question.correct) 1.0 else 0.0,
                        maxScore = 1.0,
                        topicName = question.topic,
                        explanation = question.explanation,
                        question = question.content,
                    )
                },
        )
    }

    private companion object {
        const val ITEMS = 10L
        const val QUESTION_SECONDS = 60L
        const val PERCENT = 100
        const val TENTHS = 10.0
    }
}

internal object FakePracticeData {
    val categories =
        listOf(
            CatalogCategory(1, "UTBK-SNBT", "Latihan soal UTBK-SNBT per subtes."),
            CatalogCategory(2, "TKA SMA", null),
        )

    /** Attempt id to package id of the finished attempts. */
    val finished = mapOf(901L to 104L, 902L to 201L)

    val recent =
        listOf(
            RecentAttempt(902, "Latihan Matematika TKA", null, "TKA SMA", 80.0, "Nilai", "2026-11-15T03:10:00Z"),
            RecentAttempt(901, "Latihan Penalaran Umum 3", null, "UTBK-SNBT", 60.0, "Nilai", "2026-11-14T09:30:00Z"),
        )

    fun timerOf(packageId: Long): String =
        when (packageId) {
            102L -> PracticeAttempt.TIMER_PACKAGE
            103L -> PracticeAttempt.TIMER_PER_QUESTION
            else -> PracticeAttempt.TIMER_NONE
        }

    fun packages(): List<PracticePackage> {
        val reasoning = com.gotest.app.feature.practice.domain.CatalogSubject(11, "Penalaran Umum", null)
        val quantitative =
            com.gotest.app.feature.practice.domain.CatalogSubject(12, "Pengetahuan Kuantitatif", null)
        val math = com.gotest.app.feature.practice.domain.CatalogSubject(21, "Matematika", null)
        val composition =
            listOf(com.gotest.app.feature.practice.domain.PackageComposition("Penalaran Umum", 5, "Campuran"))
        val noTimer = "Paket ini dapat dikerjakan tanpa Timer"
        return listOf(
            PracticePackage(
                101, "Latihan Penalaran Umum 1", "Lima soal penalaran untuk pemanasan.", 1, "UTBK-SNBT",
                timerLabel = noTimer, composition = composition, subjects = listOf(reasoning), topicIds = listOf(1, 2),
            ),
            PracticePackage(
                102, "Latihan Penalaran Umum 2", null, 1, "UTBK-SNBT", timerLabel = "Waktu total: 10 menit",
                timerOptional = true, composition = composition, subjects = listOf(reasoning), topicIds = listOf(1),
            ),
            PracticePackage(
                103, "Latihan Kuantitatif Cepat", null, 1, "UTBK-SNBT", timerLabel = "Waktu per soal: 1 menit",
                subjects = listOf(quantitative), topicIds = listOf(3, 4, 5),
                composition = listOf(com.gotest.app.feature.practice.domain.PackageComposition("Pengetahuan Kuantitatif", 5, "Sedang")),
            ),
            PracticePackage(
                104, "Latihan Penalaran Umum 3", null, 1, "UTBK-SNBT", completed = true, timerLabel = noTimer,
                composition = composition, subjects = listOf(reasoning), topicIds = listOf(2), allowRetry = false,
                lastAttemptId = 901, finishedCount = 1, lastFinishedAt = "2026-11-14T09:30:00Z", lastScore = 60.0,
                lastScoreLabel = "Nilai",
            ),
            PracticePackage(
                105, "Latihan Kuantitatif Lanjutan", null, 1, "UTBK-SNBT", premium = true, premiumLocked = true,
                timerLabel = noTimer, subjects = listOf(quantitative), topicIds = listOf(4),
            ),
            PracticePackage(
                201, "Latihan Matematika TKA", null, 2, "TKA SMA", completed = true, timerLabel = noTimer,
                subjects = listOf(math), topicIds = listOf(7, 8), lastAttemptId = 902, finishedCount = 1,
                lastFinishedAt = "2026-11-15T03:10:00Z", lastScore = 80.0, lastScoreLabel = "Nilai",
            ),
        )
    }

    val questions: List<FakeQuestion> = FakePracticeQuestions.all
}
