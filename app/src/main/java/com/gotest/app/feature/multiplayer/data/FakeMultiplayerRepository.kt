package com.gotest.app.feature.multiplayer.data

import com.gotest.app.core.data.fake.questionsFor
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.feature.multiplayer.domain.CancelReason
import com.gotest.app.feature.multiplayer.domain.DuelOutcome
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameCategory
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSession
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.HistoryOutcome
import com.gotest.app.feature.multiplayer.domain.JoinLookup
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import com.gotest.app.feature.multiplayer.domain.clockTicks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private const val ROOM_FRIEND_JOIN_STEP_MILLIS = 4_000L
private const val DUEL_FRIEND_JOIN_MILLIS = 5_000L
private const val FINISH_STEP_MILLIS = 3_000L
private const val EARLY_FINISH_MILLIS = 60_000L
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/**
 * In-memory Main Bareng. Lobbies fill up, hosts start and friends finish on a timer, so every
 * screen of the flow can be walked through without a server. See [DummyCodes] for the join codes.
 */
@Singleton
class FakeMultiplayerRepository
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : MultiplayerRepository {
        private val records = MutableStateFlow(MultiplayerDummyData.seedRecords(now()))
        private val activeCode = MutableStateFlow<String?>(null)
        private val answers = MutableStateFlow<Map<String, Map<String, QuestionAnswer>>>(emptyMap())

        override val connection: Flow<Boolean> = flowOf(true)

        override fun observeHistory(): Flow<List<GameHistoryItem>> =
            combine(records, answers, clockTicks()) { all, given, now ->
                all.values
                    .filter { it.currentUserId != null }
                    .mapNotNull { it.historyItem(now, given[it.code].orEmpty()) }
                    .sortedByDescending { it.playedAt }
            }.distinctUntilChanged().withFakeLatency()

        override fun observeActiveGame(): Flow<Game?> =
            combine(records, activeCode, clockTicks()) { all, code, now -> activeGame(all, code, now) }
                .distinctUntilChanged()
                .withFakeLatency()

        override suspend fun findGame(code: String): JoinLookup {
            simulateLatency()
            val now = now()
            val game = records.value[GameRules.normalizeCode(code)]?.snapshot(now)
            val active = activeGame(records.value, activeCode.value, now)
            return when {
                game == null -> JoinLookup.NotFound
                active != null && active.code != game.code -> JoinLookup.InAnotherGame(active)
                game.currentUser != null && game.isRunning -> JoinLookup.Found(game)
                game.status == GameStatus.Started -> JoinLookup.AlreadyStarted
                game.status != GameStatus.Waiting -> JoinLookup.Inactive
                game.isFull -> JoinLookup.Full
                else -> JoinLookup.Found(game)
            }
        }

        override suspend fun joinGame(code: String): Result<Unit> {
            val lookup = findGame(code)
            if (lookup !is JoinLookup.Found) return Result.failure(IllegalStateException("cannot join: $lookup"))
            val game = lookup.game
            if (game.currentUser == null) {
                val me = currentUserProfile()
                val now = now()
                records.update { all ->
                    val record = all.getValue(game.code)
                    all + (game.code to record.withStudentJoined(me, now))
                }
            }
            activeCode.value = game.code
            return Result.success(Unit)
        }

        override fun observeCatalog(): Flow<List<GameCategory>> = flowOf(MultiplayerDummyData.catalog).withFakeLatency()

        override suspend fun createGame(settings: GameSettings): Result<String> {
            simulateLatency()
            val now = now()
            if (activeGame(records.value, activeCode.value, now) != null) {
                return Result.failure(IllegalStateException("already in a game"))
            }
            val me = currentUserProfile()
            val code = newCode()
            val friends =
                when (settings.type) {
                    GameType.Duel -> {
                        listOf(Seat(MultiplayerDummyData.ardy, joinedAt = now + DUEL_FRIEND_JOIN_MILLIS))
                    }

                    GameType.Room -> {
                        MultiplayerDummyData.friendsWhoJoin.take(2).mapIndexed { index, friend ->
                            Seat(friend, joinedAt = now + ROOM_FRIEND_JOIN_STEP_MILLIS * (index + 1))
                        }
                    }
                }
            val record =
                GameRecord(
                    code = code,
                    settings = settings,
                    hostId = me.id,
                    seats = listOf(Seat(me, joinedAt = now)) + friends,
                    createdAt = now,
                    autoCancelAt = now + GameRules.AUTO_CANCEL_MILLIS,
                    currentUserId = me.id,
                    playedAt = LocalDateTime.now(),
                )
            records.update { it + (code to record) }
            activeCode.value = code
            return Result.success(code)
        }

        override fun observeGame(code: String): Flow<Game> =
            combine(records, clockTicks()) { all, now ->
                all[code]?.snapshot(now) ?: throw NoSuchElementException("unknown game $code")
            }.distinctUntilChanged().withFakeLatency()

        override suspend fun startGame(code: String): Result<Unit> {
            simulateLatency()
            val now = now()
            val game = records.value[code]?.snapshot(now)
            if (game == null || !game.canStart || !game.isHostedByCurrentUser) {
                return Result.failure(IllegalStateException("cannot start $code"))
            }
            updateRecord(code) { record ->
                record.copy(seats = record.seatsAt(now), startedAt = now, playedAt = LocalDateTime.now())
            }
            return Result.success(Unit)
        }

        override suspend fun cancelGame(code: String) {
            simulateLatency()
            val now = now()
            updateRecord(code) {
                it.copy(seats = it.seatsAt(now), cancelledAt = now, cancelReason = CancelReason.ByHost)
            }
            if (activeCode.value == code) activeCode.value = null
        }

        override suspend fun leaveGame(code: String) {
            simulateLatency()
            val record = records.value[code] ?: return
            if (record.hostId == record.currentUserId) {
                cancelGame(code)
                return
            }
            updateRecord(code) { it.withStudentLeft() }
            if (activeCode.value == code) activeCode.value = null
        }

        override suspend fun loadSession(code: String): GameSession {
            simulateLatency()
            val record = records.value[code] ?: throw NoSuchElementException("unknown game $code")
            return GameSession(
                game = record.snapshot(now()),
                questions = questionsFor(code, record.settings.questionCount),
                answers = answers.value[code].orEmpty(),
            )
        }

        override suspend fun saveAnswer(
            code: String,
            questionId: String,
            answer: QuestionAnswer,
        ) {
            answers.update { all -> all + (code to (all[code].orEmpty() + (questionId to answer))) }
        }

        override suspend fun submitAnswers(code: String) {
            simulateLatency()
            val now = now()
            updateRecord(code) { it.withStudentFinished(now) }
        }

        override fun observeResult(code: String): Flow<GameResult> =
            combine(records, answers) { all, given ->
                val record = all[code] ?: throw NoSuchElementException("unknown game $code")
                record.result(now(), questionsFor(code, record.settings.questionCount), given[code].orEmpty())
            }.distinctUntilChanged().withFakeLatency()

        private fun updateRecord(
            code: String,
            transform: (GameRecord) -> GameRecord,
        ) {
            records.update { all -> all[code]?.let { all + (code to transform(it)) } ?: all }
        }

        private fun activeGame(
            all: Map<String, GameRecord>,
            code: String?,
            now: Long,
        ): Game? =
            code
                ?.let { all[it]?.snapshot(now) }
                ?.takeIf { it.status == GameStatus.Waiting || it.status == GameStatus.Started }
                ?.takeIf { it.currentUser != null }

        private fun GameRecord.historyItem(
            now: Long,
            given: Map<String, QuestionAnswer>,
        ): GameHistoryItem? {
            val status = statusAt(now)
            if (status != GameStatus.Finished && status != GameStatus.Cancelled) return null
            val players = seatsAt(now)
            val opponent = players.firstOrNull { it.id != currentUserId }
            val outcome =
                if (status == GameStatus.Cancelled) {
                    HistoryOutcome.Cancelled
                } else {
                    result(now, questionsFor(code, settings.questionCount), given).historyOutcome()
                }
            return GameHistoryItem(
                code = code,
                settings = settings,
                playedAt = playedAt ?: LocalDateTime.now(),
                outcome = outcome,
                opponentName = opponent?.profile?.name?.takeIf { settings.type == GameType.Duel },
                playerCount = players.size,
            )
        }

        private suspend fun currentUserProfile(): PlayerProfile {
            val profile = userRepository.observeProfile().first()
            return PlayerProfile(CURRENT_USER_ID, profile.fullName, profile.avatarUrl)
        }

        private fun newCode(): String {
            var code: String
            do {
                code =
                    (1..GameRules.CODE_LENGTH)
                        .map {
                            CODE_ALPHABET[
                                Random.nextInt(
                                    CODE_ALPHABET.length,
                                ),
                            ]
                        }.joinToString("")
            } while (code in records.value)
            return code
        }

        private fun now(): Long = System.currentTimeMillis()
    }

/** The student's own lobby or game in progress, which they may reopen with its code. */
private val Game.isRunning: Boolean get() = status == GameStatus.Waiting || status == GameStatus.Started

private fun GameResult.historyOutcome(): HistoryOutcome =
    when (settings.type) {
        GameType.Room -> {
            HistoryOutcome.Ranked(rank = me?.rank ?: standings.size, players = standings.size)
        }

        GameType.Duel -> {
            when (GameRules.duelOutcome(this)) {
                DuelOutcome.Win -> HistoryOutcome.Won
                DuelOutcome.Draw -> HistoryOutcome.Draw
                else -> HistoryOutcome.Lost
            }
        }
    }

/** The student takes the scripted seat of a seeded lobby, or a new seat in anyone else's. */
private fun GameRecord.withStudentJoined(
    me: PlayerProfile,
    now: Long,
): GameRecord {
    val script = joinScript
    return if (script != null) {
        copy(
            currentUserId = script.seatId,
            autoCancelAt = now + script.autoCancelInMillis,
            startedAt = now + script.hostStartsInMillis,
            playedAt = LocalDateTime.now(),
        )
    } else {
        copy(seats = seats + Seat(me, joinedAt = now), currentUserId = me.id)
    }
}

private fun GameRecord.withStudentLeft(): GameRecord =
    if (joinScript != null) {
        copy(currentUserId = null, autoCancelAt = null, startedAt = null)
    } else {
        copy(seats = seats.filterNot { it.id == currentUserId }, currentUserId = null)
    }

/**
 * Marks the student's answers as sent. The first other player of a room "finished a minute ago";
 * everybody else finishes a few seconds apart from now on, so "Menunggu pemain lain" moves.
 */
private fun GameRecord.withStudentFinished(now: Long): GameRecord {
    val start = startedAt ?: now
    var nextOther = 0
    val roomOfMany = seats.size > 2
    val updated =
        seats.map { seat ->
            when {
                seat.id == currentUserId -> {
                    seat.copy(finishedAt = now)
                }

                seat.finishedAt != null -> {
                    seat
                }

                else -> {
                    val finishedAt =
                        if (roomOfMany && nextOther == 0) {
                            maxOf(start, now - EARLY_FINISH_MILLIS)
                        } else {
                            now + FINISH_STEP_MILLIS * (nextOther + 1)
                        }
                    nextOther++
                    seat.copy(finishedAt = finishedAt)
                }
            }
        }
    return copy(seats = updated)
}
