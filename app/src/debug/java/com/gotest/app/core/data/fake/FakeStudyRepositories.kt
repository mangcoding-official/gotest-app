package com.gotest.app.core.data.fake

import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.ExamSession
import com.gotest.app.core.model.GlobalLeaderboard
import com.gotest.app.core.model.LeaderboardEntry
import com.gotest.app.core.model.PackageLeaderboard
import com.gotest.app.core.model.PackageRanking
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.core.model.SeasonalTryout
import com.gotest.app.core.model.SectionResult
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.core.model.Subject
import com.gotest.app.core.model.TopicAccuracy
import com.gotest.app.core.model.isAnsweredCorrectly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val PERCENT = 100
private const val SCORE_SCALE = 900
private const val RANK_STEP = 30
private const val SEASONAL_HOURS = 24L
private const val SEASONAL_MINUTES = 41L
private const val SECONDS_PER_MINUTE = 60

@Singleton
class FakeStudyPackageRepository
    @Inject
    constructor() : StudyPackageRepository {
        private val seasonalEndsAt =
            System.currentTimeMillis() + TimeUnit.HOURS.toMillis(SEASONAL_HOURS) +
                TimeUnit.MINUTES.toMillis(SEASONAL_MINUTES)

        override fun observeRecommended(type: PackageType): Flow<List<StudyPackage>> =
            flowOf(
                when (type) {
                    PackageType.Practice -> {
                        listOf(PackageIds.TOPIC_APP_DEV, PackageIds.TOPIC_PROGRAMMING, PackageIds.LATIHAN_MTK_SMA)
                    }

                    PackageType.Tryout -> {
                        listOf(PackageIds.TRYOUT_UTBK_1, PackageIds.TRYOUT_UTBK_2)
                    }
                }.map(DummyCatalog::packageById),
            ).withFakeLatency()

        override fun observeInProgress(): Flow<StudyPackage?> =
            flowOf(DummyCatalog.packageById(PackageIds.LATIHAN_SEPT_1)).withFakeLatency()

        override fun observeCategories(type: PackageType): Flow<List<StudyCategory>> =
            flowOf(DummyCatalog.practiceCategories).withFakeLatency()

        override fun observeCategory(categoryId: String): Flow<StudyCategory> =
            flowOf(
                DummyCatalog.practiceCategories.firstOrNull { it.id == categoryId }
                    ?: DummyCatalog.practiceCategories[1],
            ).withFakeLatency()

        override fun observeSubjects(categoryId: String): Flow<List<Subject>> =
            flowOf(DummyCatalog.subjects(categoryId)).withFakeLatency()

        override fun observeSubject(subjectId: String): Flow<Subject> =
            flowOf(DummyCatalog.subjectById(subjectId)).withFakeLatency()

        override fun observeTopics(subjectId: String) = flowOf(DummyCatalog.topics(subjectId)).withFakeLatency()

        override fun observePackages(
            type: PackageType,
            levelLabel: String?,
        ): Flow<List<StudyPackage>> =
            flowOf(
                DummyCatalog.packages.filter { pkg ->
                    pkg.type == type &&
                        (levelLabel == null || pkg.levelLabel == levelLabel || pkg.examLabel == levelLabel)
                },
            ).withFakeLatency()

        override fun observeLevels(): Flow<List<String>> = flowOf(DummyCatalog.levels).withFakeLatency()

        override fun observePackageDetail(packageId: String): Flow<StudyPackageDetail> =
            flowOf(DummyCatalog.detail(packageId)).withFakeLatency()

        override fun observeSeasonalTryout(): Flow<SeasonalTryout?> =
            flowOf(
                SeasonalTryout(
                    title = "Seasonal Tryout",
                    description = "Ini adalah tryout yang dibatasi pada periode tertentu saja.",
                    endsAtEpochMillis = seasonalEndsAt,
                    packages =
                        DummyCatalog.topics("cat_slta_sub_4").take(4).map { topic ->
                            DummyCatalog.packageById(topic.packageId).copy(
                                id = topic.packageId,
                                title = topic.name,
                                type = PackageType.Tryout,
                                rank = if (topic.attempts > 0) 4 else null,
                                participants = if (topic.attempts > 0) 124 else null,
                            )
                        },
                ),
            ).withFakeLatency()
    }

@Singleton
class FakeExamRepository
    @Inject
    constructor() : ExamRepository {
        private val sessions = MutableStateFlow<Map<String, ExamSession>>(emptyMap())
        private val results = MutableStateFlow(DummyResults.seededResults.associateBy { it.attemptId })
        private val reviews = MutableStateFlow(DummyResults.seededReviews)
        private val history = MutableStateFlow(DummyResults.history)

        init {
            // Matches the design's in-progress latihan: questions 1–3 answered, 7 marked.
            val pkg = DummyCatalog.packageById(PackageIds.LATIHAN_SEPT_1)
            val questions = questionsFor(pkg.id, pkg.questionCount)
            val seeded =
                ExamSession(
                    attemptId = "att_open_sept_1",
                    packageId = pkg.id,
                    title = pkg.title,
                    type = pkg.type,
                    questions = questions,
                    answers =
                        questions.take(3).associate { q ->
                            q.id to QuestionAnswer(selectedKeys = q.correctKeys)
                        },
                    markedQuestionIds = setOf(questions[6].id),
                    remainingSeconds = null,
                )
            sessions.value = mapOf(seeded.attemptId to seeded)
        }

        override suspend fun startOrResume(packageId: String): ExamSession {
            simulateLatency()
            sessions.value.values
                .firstOrNull { it.packageId == packageId }
                ?.let { return it }
            val pkg = DummyCatalog.packageById(packageId)
            val session =
                ExamSession(
                    attemptId = "att_${UUID.randomUUID()}",
                    packageId = pkg.id,
                    title = pkg.title,
                    type = pkg.type,
                    questions = questionsFor(pkg.id, pkg.questionCount),
                    answers = emptyMap(),
                    markedQuestionIds = emptySet(),
                    remainingSeconds =
                        if (pkg.type == PackageType.Tryout) pkg.durationMinutes?.times(SECONDS_PER_MINUTE) else null,
                )
            sessions.update { it + (session.attemptId to session) }
            return session
        }

        override suspend fun saveAnswer(
            attemptId: String,
            questionId: String,
            answer: QuestionAnswer,
        ) {
            sessions.update { all ->
                val session = all[attemptId] ?: return@update all
                all + (attemptId to session.copy(answers = session.answers + (questionId to answer)))
            }
        }

        override suspend fun setMarked(
            attemptId: String,
            questionId: String,
            marked: Boolean,
        ) {
            sessions.update { all ->
                val session = all[attemptId] ?: return@update all
                val ids = if (marked) session.markedQuestionIds + questionId else session.markedQuestionIds - questionId
                all + (attemptId to session.copy(markedQuestionIds = ids))
            }
        }

        override suspend fun submit(attemptId: String): String {
            simulateLatency()
            val session = sessions.value[attemptId] ?: return attemptId
            val pkg = DummyCatalog.packageById(session.packageId)
            val graded = session.questions.map { q -> QuestionReview(q, session.answers[q.id] ?: QuestionAnswer()) }
            val correct = graded.count { it.isCorrect }
            val empty = graded.count { it.isEmpty }
            val score = (correct.toFloat() / graded.size * SCORE_SCALE).roundToInt()
            val result =
                AttemptResult(
                    attemptId = attemptId,
                    packageId = pkg.id,
                    title = pkg.title,
                    description = pkg.description,
                    examLabel = pkg.examLabel,
                    type = pkg.type,
                    score = if (pkg.type == PackageType.Practice) correct else score,
                    rank = (SCORE_SCALE - score) / RANK_STEP + 1,
                    participants = pkg.participants ?: DummyResults.DEFAULT_PARTICIPANTS,
                    correct = correct,
                    wrong = graded.size - correct - empty,
                    empty = empty,
                    topicAccuracy =
                        graded.groupBy { it.question.topic }.map { (topic, items) ->
                            TopicAccuracy(topic, items.count { it.isCorrect } * PERCENT / items.size)
                        },
                    weakestTopic =
                        graded
                            .groupBy { it.question.topic }
                            .minByOrNull { (_, items) -> items.count { it.isCorrect }.toFloat() / items.size }
                            ?.key,
                    sections = listOf(SectionResult(pkg.title, correct, graded.size - correct - empty, empty)),
                    completedOn = LocalDate.now(),
                    durationSeconds = DummyResults.DEFAULT_DURATION_SECONDS,
                )
            results.update { it + (attemptId to result) }
            reviews.update { it + (attemptId to graded) }
            history.update {
                listOf(
                    AttemptSummary(
                        attemptId = attemptId,
                        packageId = pkg.id,
                        title = pkg.title,
                        type = pkg.type,
                        examLabel = pkg.examLabel,
                        completedOn = result.completedOn,
                        durationSeconds = result.durationSeconds,
                        score = result.score,
                    ),
                ) + it
            }
            sessions.update { it - attemptId }
            return attemptId
        }

        override fun observeResult(attemptId: String): Flow<AttemptResult> =
            results
                .map { it[attemptId] ?: DummyResults.resultFor(attemptId, history.value) }
                .filterNotNull()
                .withFakeLatency()

        override fun observeReview(attemptId: String): Flow<List<QuestionReview>> =
            reviews.map { it[attemptId] ?: it.getValue(DummyResults.TRYOUT_ATTEMPT) }.withFakeLatency()

        override fun observeHistory(): Flow<List<AttemptSummary>> = history.withFakeLatency()
    }

@Singleton
class FakeLeaderboardRepository
    @Inject
    constructor() : LeaderboardRepository {
        override fun observeGlobal(): Flow<GlobalLeaderboard> =
            flowOf(
                GlobalLeaderboard(
                    myRank = 12,
                    myRankChange = 1,
                    totalUsers = 12_512,
                    entries = DummyResults.globalEntries,
                ),
            ).withFakeLatency()

        override fun observePackageRankings(): Flow<List<PackageRanking>> =
            flowOf(DummyResults.packageRankings).withFakeLatency()

        override fun observePackageLeaderboard(packageId: String): Flow<PackageLeaderboard> {
            val ranking =
                DummyResults.packageRankings.firstOrNull { it.packageId == packageId }
                    ?: DummyResults.packageRankings.first()
            return flowOf(
                PackageLeaderboard(
                    packageId = ranking.packageId,
                    title = ranking.title,
                    examLabel = ranking.examLabel,
                    questionCount = ranking.questionCount,
                    durationMinutes = ranking.durationMinutes,
                    entries = DummyResults.packageEntries,
                ),
            ).withFakeLatency()
        }
    }

internal fun leaderboardEntry(
    rank: Int,
    name: String,
    avatar: String,
    points: Int,
    attempts: Int? = null,
    finishSeconds: Int? = null,
    rankChange: Int = 0,
    isCurrentUser: Boolean = false,
) = LeaderboardEntry(
    rank = rank,
    name = name,
    avatarUrl = drawableUri(avatar),
    points = points,
    attempts = attempts,
    finishSeconds = finishSeconds,
    rankChange = rankChange,
    isCurrentUser = isCurrentUser,
)
