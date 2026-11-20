package com.gotest.app.feature.tryout.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.data.FakePracticeQuestions
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.CatalogCategory
import com.gotest.app.feature.practice.domain.RecentAttempt
import com.gotest.app.feature.practice.domain.ScoringProfile
import com.gotest.app.feature.practice.domain.StudentStats
import com.gotest.app.feature.tryout.domain.LeaderboardRow
import com.gotest.app.feature.tryout.domain.SessionLeaderboard
import com.gotest.app.feature.tryout.domain.SessionLeaderboardEntry
import com.gotest.app.feature.tryout.domain.SessionSection
import com.gotest.app.feature.tryout.domain.TryoutActiveSection
import com.gotest.app.feature.tryout.domain.TryoutAttempt
import com.gotest.app.feature.tryout.domain.TryoutCatalogPage
import com.gotest.app.feature.tryout.domain.TryoutItem
import com.gotest.app.feature.tryout.domain.TryoutPackage
import com.gotest.app.feature.tryout.domain.TryoutQuestion
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutResult
import com.gotest.app.feature.tryout.domain.TryoutResultSection
import com.gotest.app.feature.tryout.domain.TryoutSectionInfo
import com.gotest.app.feature.tryout.domain.TryoutSectionSummary
import com.gotest.app.feature.tryout.domain.TryoutSession
import com.gotest.app.feature.tryout.domain.TryoutSessionPage
import com.gotest.app.feature.tryout.domain.TryoutWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Debug data for Try Out (API module D; debug builds only, T-026 criterion 8). UTBK-SNBT has a Try Out of three
 * subtests (5 minutes each, the five Latihan questions; the fourth has a reading), one done once that cannot be
 * done again, one that opens later, and a Premium one locked for this Gratis account. Sessions: one running that
 * Gilang registered for, one coming he can register for, and one finished with its ranking. Every call fails like
 * a dropped connection while the phone has no network. Messages are the server's (TryoutAttemptController,
 * TryoutSessionController).
 */
@Suppress("TooManyFunctions")
@Singleton
class FakeTryoutRepository
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : TryoutRepository {
        private val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
        private val packages = FakeTryoutData.packages(now).toMutableList()
        private val sessions = FakeTryoutData.sessions(now).toMutableList()
        private val attempts = mutableMapOf<Long, FakeTryoutAttempt>()
        private var nextId = FIRST_ATTEMPT

        override suspend fun catalog(): Result<TryoutCatalogPage> =
            call {
                Result.success(
                    TryoutCatalogPage(
                        sessions = sessions.map(::withAttempt),
                        serverNow = Instant.now().toString(),
                        categories = listOf(CatalogCategory(1, "UTBK-SNBT", null)),
                        packages = packages.map(::withProgress),
                        recentAttempts =
                            listOf(RecentAttempt(950, "Try Out UTBK-SNBT 0", null, "UTBK-SNBT", 612.0, null, "2026-11-12T08:00:00Z")),
                        stats = StudentStats(questionsDone = 480, accuracy = 68, packagesDone = 19),
                    ),
                )
            }

        override suspend fun start(packageId: Long): Result<TryoutAttempt> =
            call start@{
                val item = packages.firstOrNull { it.id == packageId } ?: return@start notFound()
                attempts.values.firstOrNull { it.packageId == packageId && it.running && !it.inSession }?.let {
                    return@start Result.success(it.snapshot())
                }
                when {
                    item.premiumLocked -> refuse(FORBIDDEN, "premium_only", "Paket ini khusus Premium.")
                    item.windowState == TryoutWindow.STATE_UPCOMING -> validation("package", "Try Out ini dibuka pada ${FakeTryoutData.wib(item.window?.opensAt)} WIB.")
                    item.resultOnlyId != null -> validation("package", "Paket ini tidak dapat dikerjakan ulang.")
                    else -> Result.success(newAttempt(packageId, inSession = false).snapshot())
                }
            }

        override suspend fun attempt(attemptId: Long): Result<TryoutAttempt> =
            call { attempts[attemptId]?.let { Result.success(it.snapshot()) } ?: notFound() }

        override suspend fun begin(attemptId: Long): Result<TryoutAttempt> =
            call { attempts[attemptId]?.apply { begin() }?.let { Result.success(it.snapshot()) } ?: notFound() }

        override suspend fun answer(
            attemptId: Long,
            itemId: Long,
            answer: AnswerData,
        ): Result<Unit> = call { attempts[attemptId]?.answer(itemId, answer) ?: notFound() }

        override suspend fun flag(
            attemptId: Long,
            itemId: Long,
            flagged: Boolean,
        ): Result<Unit> = call { attempts[attemptId]?.flag(itemId, flagged) ?: notFound() }

        override suspend fun finishSection(
            attemptId: Long,
            sectionId: Long?,
        ): Result<TryoutAttempt> =
            call {
                val current = attempts[attemptId] ?: return@call notFound()
                current.finishSection(sectionId)
                finished(current)
                Result.success(current.snapshot())
            }

        override suspend fun submit(attemptId: Long): Result<TryoutAttempt> =
            call {
                val current = attempts[attemptId] ?: return@call notFound()
                current.submit()
                finished(current)
                Result.success(current.snapshot())
            }

        override suspend fun result(attemptId: Long): Result<TryoutResult> =
            call {
                val current = attempts[attemptId]?.apply { closeIfLate() }
                when {
                    current == null && attemptId == FakeTryoutData.DONE_ATTEMPT -> Result.success(FakeTryoutData.doneResult())
                    current == null || current.running || current.status == TryoutAttempt.STATUS_CANCELLED -> notFound()
                    else -> Result.success(current.result(packages.firstOrNull { it.id == current.packageId }?.name.orEmpty()))
                }
            }

        override suspend fun session(sessionId: Long): Result<TryoutSessionPage> =
            call {
                val session = sessions.firstOrNull { it.id == sessionId } ?: return@call notFound()
                Result.success(TryoutSessionPage(withAttempt(session), canStart = true))
            }

        override suspend fun register(sessionId: Long): Result<String?> =
            call {
                val index = sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 } ?: return@call notFound()
                val session = sessions[index]
                if (session.state != TryoutSession.SCHEDULED) return@call validation("session", "Pendaftaran sesi sudah ditutup.")
                sessions[index] = session.copy(registered = true, registeredCount = session.registeredCount + 1)
                Result.success("Anda terdaftar pada sesi ini. Kembali saat sesi dimulai untuk mengerjakan.")
            }

        override suspend fun unregister(sessionId: Long): Result<String?> =
            call {
                val index = sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 } ?: return@call notFound()
                val session = sessions[index]
                sessions[index] = session.copy(registered = false, registeredCount = (session.registeredCount - 1).coerceAtLeast(0))
                Result.success("Pendaftaran sesi dibatalkan.")
            }

        override suspend fun startSession(sessionId: Long): Result<Long> =
            call {
                val session = sessions.firstOrNull { it.id == sessionId } ?: return@call notFound()
                attempts.values.firstOrNull { it.sessionId == sessionId }?.let { return@call Result.success(it.id) }
                if (session.state != TryoutSession.RUNNING || !session.registered) {
                    return@call validation("session", "Sesi ini belum dapat dimulai.")
                }
                Result.success(newAttempt(session.packageId, inSession = true, sessionId = sessionId).id)
            }

        override suspend fun sessionLeaderboard(sessionId: Long): Result<SessionLeaderboard> =
            call {
                val session = sessions.firstOrNull { it.id == sessionId } ?: return@call notFound()
                if (session.state != TryoutSession.CLOSED) return@call Result.success(SessionLeaderboard(availableAt = session.endsAt))
                Result.success(FakeTryoutData.leaderboard())
            }

        private fun newAttempt(
            packageId: Long,
            inSession: Boolean,
            sessionId: Long? = null,
        ): FakeTryoutAttempt {
            val fresh = FakeTryoutAttempt(nextId++, packageId, inSession, sessionId)
            attempts[fresh.id] = fresh
            return fresh
        }

        /** A finished attempt counts on its package or session, as the catalog shows it after. */
        private fun finished(attempt: FakeTryoutAttempt) {
            if (attempt.running || attempt.status == TryoutAttempt.STATUS_CANCELLED) return
            packages.replaceAll { item ->
                if (item.id != attempt.packageId || attempt.inSession) {
                    item
                } else {
                    item.copy(
                        completed = true,
                        finishedCount = item.finishedCount + 1,
                        lastAttemptId = attempt.id,
                        lastFinishedAt = Instant.now().toString(),
                        lastScore = attempt.score(),
                    )
                }
            }
        }

        private fun withProgress(item: TryoutPackage): TryoutPackage {
            val running = attempts.values.firstOrNull { it.packageId == item.id && it.running && !it.inSession }
            return item.copy(continueAttemptId = running?.id)
        }

        private fun withAttempt(session: TryoutSession): TryoutSession {
            val attempt = attempts.values.firstOrNull { it.sessionId == session.id }?.apply { closeIfLate() } ?: return session
            val done = !attempt.running
            return session.copy(
                attemptId = attempt.id,
                attemptStatus = attempt.status,
                attemptDone = done,
                score = if (done) attempt.score() else null,
                submittedAt = if (done) Instant.now().toString() else null,
            )
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

        companion object {
            private const val FIRST_ATTEMPT = 3001L
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

/** One attempt: three subtests in order, each with its own deadline once begun (DEC-062 to DEC-067). */
internal class FakeTryoutAttempt(
    val id: Long,
    val packageId: Long,
    val inSession: Boolean,
    val sessionId: Long?,
) {
    private val questions = FakePracticeQuestions.all
    private val sectionItems = listOf(listOf(0, 1), listOf(2, 3), listOf(4))
    private val itemIds = questions.indices.map { id * ITEMS + it + 1 }
    private val statuses = MutableList(sectionItems.size) { TryoutSectionSummary.PENDING }
    private val deadlines = arrayOfNulls<Instant>(sectionItems.size)
    private val answers = mutableMapOf<Long, AnswerData>()
    private val flags = mutableSetOf<Long>()
    var status = TryoutAttempt.STATUS_STARTED

    val running: Boolean get() = status == TryoutAttempt.STATUS_STARTED
    private val activeIndex: Int get() = statuses.indexOf(TryoutSectionSummary.STARTED)

    /** A subtest whose time ran out is locked and the next one starts by itself; the last ends the Try Out. */
    fun closeIfLate() {
        val index = activeIndex
        val due = deadlines.getOrNull(index) ?: return
        if (running && Instant.now().isAfter(due)) {
            statuses[index] = FINISHED
            if (index == sectionItems.lastIndex) status = "auto_submitted" else startSection(index + 1)
        }
    }

    fun begin() {
        closeIfLate()
        if (running && activeIndex < 0) statuses.indexOf(TryoutSectionSummary.PENDING).takeIf { it >= 0 }?.let(::startSection)
    }

    private fun startSection(index: Int) {
        statuses[index] = TryoutSectionSummary.STARTED
        deadlines[index] = Instant.now().plusSeconds(SECTION_SECONDS)
    }

    fun answer(
        itemId: Long,
        answer: AnswerData,
    ): Result<Unit> {
        closeIfLate()
        return when {
            !running -> FakeTryoutRepository.validation("answer", "Try Out ini sudah tidak dapat diubah.")
            !activeItem(itemId) -> FakeTryoutRepository.validation("answer", "Section soal ini sudah dikunci.")
            else -> Result.success(Unit).also { answers[itemId] = answer }
        }
    }

    fun flag(
        itemId: Long,
        flagged: Boolean,
    ): Result<Unit> {
        closeIfLate()
        if (!running || !activeItem(itemId)) return FakeTryoutRepository.validation("question", "Section soal ini sudah dikunci.")
        if (flagged) flags += itemId else flags -= itemId
        return Result.success(Unit)
    }

    private fun activeItem(itemId: Long): Boolean = activeIndex >= 0 && sectionItems[activeIndex].any { itemIds[it] == itemId }

    fun finishSection(sectionId: Long?) {
        closeIfLate()
        val index = activeIndex
        if (!running || index < 0 || (sectionId != null && sectionId != sectionIdOf(index))) return
        statuses[index] = FINISHED
        if (index == sectionItems.lastIndex) status = "submitted" else startSection(index + 1)
    }

    fun submit() {
        closeIfLate()
        if (!running) return
        val untouched = statuses.all { it == TryoutSectionSummary.PENDING }
        status = if (untouched && !inSession) TryoutAttempt.STATUS_CANCELLED else "submitted"
        statuses.replaceAll { FINISHED }
    }

    private fun sectionIdOf(index: Int): Long = id * ITEMS + SECTION_IDS + index

    fun snapshot(): TryoutAttempt {
        closeIfLate()
        val index = activeIndex
        val summaries =
            sectionItems.indices.map { section ->
                TryoutSectionSummary(
                    id = sectionIdOf(section),
                    title = FakeTryoutData.SECTION_TITLES[section],
                    orderNumber = section + 1,
                    status = statuses[section],
                    durationSeconds = SECTION_SECONDS.toInt(),
                    questionCount = sectionItems[section].size,
                )
            }
        return TryoutAttempt(
            id = id,
            status = status,
            inSession = inSession,
            serverNow = Instant.now().toString(),
            activeSection = if (running && index >= 0) activeSection(index) else null,
            nextSection = if (running && index < 0) summaries.firstOrNull { it.status == TryoutSectionSummary.PENDING } else null,
            sections = summaries,
        )
    }

    private fun activeSection(index: Int) =
        TryoutActiveSection(
            id = sectionIdOf(index),
            title = FakeTryoutData.SECTION_TITLES[index],
            orderNumber = index + 1,
            durationSeconds = SECTION_SECONDS.toInt(),
            deadlineAt = deadlines[index]?.toString(),
            items =
                sectionItems[index].mapIndexed { position, questionIndex ->
                    val itemId = itemIds[questionIndex]
                    val answer = answers[itemId]
                    val question = questions[questionIndex]
                    TryoutItem(
                        id = itemId,
                        questionId = question.id,
                        displayOrder = position + 1,
                        answeredAt = answer?.let { Instant.now().toString() },
                        isFlagged = itemId in flags,
                        selectedOption = answer?.selectedOption,
                        selectedOptions = answer?.selectedOptions.orEmpty(),
                        statementAnswers = answer?.statementAnswers.orEmpty(),
                        question = TryoutQuestion(question.type, question.content),
                    )
                },
        )

    private fun correct(index: Int): Boolean = answers[itemIds[index]] == questions[index].correct

    fun score(): Double = (questions.indices.count(::correct) * SCALE / questions.size).toDouble().roundToInt().toDouble()

    fun result(packageName: String): TryoutResult {
        val perQuestion = SCALE.toDouble() / questions.size
        return TryoutResult(
            id = id,
            packageId = packageId,
            packageName = packageName,
            status = status,
            score = score(),
            scoreLabel = "Skor Try Out",
            ranked = true,
            rank = if (inSession) null else RANK,
            totalParticipants = if (inSession) null else PARTICIPANTS,
            sessionId = sessionId,
            rankingAvailable = !inSession,
            rankingAvailableAt = null,
            scoringProfile = ScoringProfile(scale = SCALE.toDouble()),
            correctCount = questions.indices.count(::correct),
            wrongCount = questions.indices.count { itemIds[it] in answers && !correct(it) },
            emptyCount = questions.indices.count { itemIds[it] !in answers },
            sections =
                sectionItems.mapIndexed { section, members ->
                    TryoutResultSection(
                        id = sectionIdOf(section),
                        title = FakeTryoutData.SECTION_TITLES[section],
                        correctCount = members.count(::correct),
                        wrongCount = members.count { itemIds[it] in answers && !correct(it) },
                        emptyCount = members.count { itemIds[it] !in answers },
                        sectionScore = members.count(::correct) * perQuestion,
                        maxScore = members.size * perQuestion,
                    )
                },
        )
    }

    private companion object {
        const val ITEMS = 100L
        const val SECTION_IDS = 50L
        const val SECTION_SECONDS = 5L * 60
        const val FINISHED = "completed"
        const val SCALE = 1000
        const val RANK = 12
        const val PARTICIPANTS = 87
    }
}

internal object FakeTryoutData {
    val SECTION_TITLES = listOf("Penalaran Umum", "Literasi Bahasa Indonesia", "Penalaran Matematika")
    const val DONE_ATTEMPT = 951L

    private val sections =
        listOf(
            TryoutSectionInfo(1, SECTION_TITLES[0], 2, 5),
            TryoutSectionInfo(2, SECTION_TITLES[1], 2, 5),
            TryoutSectionInfo(3, SECTION_TITLES[2], 1, 5),
        )

    private val board =
        listOf(
            LeaderboardRow(1, "Nadia Putri", 845.0, "2026-11-12T08:30:00Z", 4_210),
            LeaderboardRow(2, "Rafi Ananda", 812.0, "2026-11-12T09:02:00Z", 4_480),
            LeaderboardRow(3, "Salsa Maharani", 790.0, "2026-11-13T07:15:00Z", 4_600),
        )

    private const val RULE = "Timer wajib per subtes. Subtes yang waktunya habis dikunci dan subtes berikutnya dimulai otomatis."

    fun wib(iso: String?): String =
        iso?.let { java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", java.util.Locale.forLanguageTag("id-ID")).withZone(java.time.ZoneId.of("Asia/Jakarta")).format(Instant.parse(it)) }.orEmpty()

    fun packages(now: Instant): List<TryoutPackage> =
        listOf(
            TryoutPackage(301, "Try Out UTBK-SNBT 1", "Simulasi tiga subtes dengan timer wajib per subtes.", 1, "UTBK-SNBT", ruleLabel = RULE, sections = sections, leaderboard = board),
            TryoutPackage(
                302, "Try Out UTBK-SNBT 0", null, 1, "UTBK-SNBT", completed = true, allowRetry = false, finishedCount = 1,
                lastFinishedAt = "2026-11-12T08:00:00Z", lastScore = 612.0, rank = 31, totalParticipants = 140, lastAttemptId = DONE_ATTEMPT,
                ruleLabel = RULE, sections = sections, leaderboard = board,
            ),
            TryoutPackage(
                303, "Try Out Akbar Desember", null, 1, "UTBK-SNBT", ruleLabel = RULE, sections = sections,
                window = TryoutWindow(TryoutWindow.STATE_UPCOMING, opensAt = now.plus(3, ChronoUnit.DAYS).toString(), closesAt = now.plus(4, ChronoUnit.DAYS).toString()),
            ),
            TryoutPackage(304, "Try Out Premium Intensif", null, 1, "UTBK-SNBT", premium = true, premiumLocked = true, ruleLabel = RULE, sections = sections),
        )

    fun sessions(now: Instant): List<TryoutSession> {
        val sessionSections = sections.mapIndexed { index, section -> SessionSection(section.id, section.title, index + 1, 300) }
        return listOf(
            TryoutSession(
                401, "Sesi Serentak November", null, 301, "Try Out UTBK-SNBT 1", state = TryoutSession.RUNNING,
                startsAt = now.minus(30, ChronoUnit.MINUTES).toString(), endsAt = now.plus(2, ChronoUnit.HOURS).toString(),
                capacity = 200, registeredCount = 154, registered = true, sections = sessionSections,
            ),
            TryoutSession(
                402, "Sesi Serentak Desember", null, 301, "Try Out UTBK-SNBT 1", state = TryoutSession.SCHEDULED,
                startsAt = now.plus(2, ChronoUnit.DAYS).toString(), endsAt = now.plus(2, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS).toString(),
                capacity = 200, registeredCount = 63, canRegister = true, sections = sessionSections,
            ),
            TryoutSession(
                403, "Sesi Serentak Oktober", null, 301, "Try Out UTBK-SNBT 1", state = TryoutSession.CLOSED,
                startsAt = "2026-10-25T01:00:00Z", endsAt = "2026-10-25T04:00:00Z", capacity = 200, registeredCount = 180,
                registered = true, attemptId = 952, attemptDone = true, score = 688.0, submittedAt = "2026-10-25T03:20:00Z",
                rank = 21, totalParticipants = 174, sections = sessionSections,
            ),
        )
    }

    fun doneResult() =
        TryoutResult(
            id = DONE_ATTEMPT, packageId = 302, packageName = "Try Out UTBK-SNBT 0", status = "submitted", score = 612.0,
            scoreLabel = "Skor Try Out", ranked = true, rank = 31, totalParticipants = 140, rankingAvailable = true,
            scoringProfile = ScoringProfile(scale = 1000.0), correctCount = 3, wrongCount = 1, emptyCount = 1,
            sections =
                listOf(
                    TryoutResultSection(1, SECTION_TITLES[0], 1, 1, 0, 200.0, 400.0),
                    TryoutResultSection(2, SECTION_TITLES[1], 2, 0, 0, 400.0, 400.0),
                    TryoutResultSection(3, SECTION_TITLES[2], 0, 0, 1, 0.0, 200.0),
                ),
        )

    fun leaderboard() =
        SessionLeaderboard(
            entries = board.map { SessionLeaderboardEntry(it.rank, it.name, it.score, it.durationUsed, it.submittedAt) },
            userEntry = SessionLeaderboardEntry(21, "Gilang Permana", 688.0, 5_100, "2026-10-25T03:20:00Z", isCurrentUser = true),
            totalParticipants = 174,
        )
}
