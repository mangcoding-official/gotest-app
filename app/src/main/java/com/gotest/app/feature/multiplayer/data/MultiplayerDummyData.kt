package com.gotest.app.feature.multiplayer.data

import com.gotest.app.core.data.fake.drawableUri
import com.gotest.app.feature.multiplayer.domain.GameCategory
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.GameSubtest
import com.gotest.app.feature.multiplayer.domain.GameTopic
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.PlayerStanding
import com.gotest.app.feature.multiplayer.domain.QuestionOutcome
import java.time.LocalDateTime

/** Someone who can sit in a game. */
internal data class PlayerProfile(
    val id: String,
    val name: String,
    val avatarUrl: String?,
)

/** How a simulated opponent performs: share of questions right and share of the time used. */
internal data class OpponentPerformance(
    val correctShare: Float,
    val timeShare: Float,
)

/** Dummy codes the join sheet recognises. Any other code is "Kode tidak ditemukan". */
internal object DummyCodes {
    const val FOUND = "KTR4QZ"
    const val STARTED = "MNB7RT"
    const val FULL = "FULL10"
    const val INACTIVE = "BATAL1"
    const val HISTORY_DUEL_WIN = "DUEL22"
    const val HISTORY_ROOM = "ROOM21"
    const val HISTORY_DUEL_DRAW = "SERI20"
}

/** Seat id of the signed-in student in games they created. */
internal const val CURRENT_USER_ID = "me"

internal object MultiplayerDummyData {
    private const val UTBK = "UTBK-SNBT"
    private const val CPNS = "CPNS"
    private const val PENALARAN_MATEMATIKA = "Penalaran Matematika"

    val gilang = PlayerProfile("gilang", "Gilang Permana", drawableUri("avatar_gilang"))
    val ardy = PlayerProfile("ardy", "Ardy Pratama", drawableUri("avatar_ardy"))
    val nadia = PlayerProfile("nadia", "Nadia Putri", drawableUri("avatar_nadia"))
    val rizky = PlayerProfile("rizky", "Rizky Ramadhan", drawableUri("avatar_rizky"))
    val sinta = PlayerProfile("sinta", "Sinta Lestari", drawableUri("avatar_sinta"))
    val bima = PlayerProfile("bima", "Bima Saputra", drawableUri("avatar_bima"))
    val michael = PlayerProfile("michael", "Michael Tan", drawableUri("avatar_michael"))
    val emily = PlayerProfile("emily", "Emily Hartono", drawableUri("avatar_emily"))
    val john = PlayerProfile("john", "John Siregar", drawableUri("avatar_john"))
    private val tari = PlayerProfile("tari", "Tari Wulandari", null)

    /** The signed-in student as seeded history shows them (the default profile). */
    val me = PlayerProfile(CURRENT_USER_ID, "Gilang Permana", drawableUri("avatar_gilang"))

    /** Friends who join a freshly created game, in order. */
    val friendsWhoJoin = listOf(ardy, nadia, rizky)

    val performance: Map<String, OpponentPerformance> =
        mapOf(
            ardy.id to OpponentPerformance(correctShare = 0.9f, timeShare = 0.85f),
            nadia.id to OpponentPerformance(correctShare = 0.8f, timeShare = 0.89f),
            gilang.id to OpponentPerformance(correctShare = 0.7f, timeShare = 0.94f),
            rizky.id to OpponentPerformance(correctShare = 0.7f, timeShare = 0.99f),
            sinta.id to OpponentPerformance(correctShare = 0.55f, timeShare = 1f),
            bima.id to OpponentPerformance(correctShare = 0.45f, timeShare = 1f),
        )
    val defaultPerformance = OpponentPerformance(correctShare = 0.6f, timeShare = 0.9f)

    val catalog: List<GameCategory> =
        listOf(
            GameCategory(
                name = UTBK,
                subtests =
                    listOf(
                        subtest(
                            PENALARAN_MATEMATIKA,
                            "Aljabar" to 24,
                            "Logaritma" to 53,
                            "Statistika" to 28,
                            "Peluang" to 19,
                            "Geometri" to 31,
                        ),
                        subtest(
                            "Penalaran Umum",
                            "Penalaran Deduktif" to 34,
                            "Penalaran Induktif" to 29,
                            "Penalaran Kuantitatif" to 26,
                        ),
                        subtest(
                            "Pengetahuan Kuantitatif",
                            "Bilangan" to 31,
                            "Aljabar Dasar" to 27,
                            "Geometri Dasar" to 22,
                        ),
                        subtest(
                            "Literasi Bahasa Indonesia",
                            "Ide Pokok" to 33,
                            "Simpulan Bacaan" to 26,
                            "Ejaan dan Tanda Baca" to 21,
                        ),
                        subtest("Literasi Bahasa Inggris", "Main Idea" to 25, "Vocabulary" to 23, "Inference" to 18),
                    ),
            ),
            GameCategory(
                name = CPNS,
                subtests =
                    listOf(
                        subtest(
                            "Tes Wawasan Kebangsaan",
                            "Pancasila" to 30,
                            "UUD 1945" to 28,
                            "Bhinneka Tunggal Ika" to 20,
                        ),
                        subtest("Tes Intelegensia Umum", "Verbal" to 35, "Numerik" to 32, "Figural" to 24),
                        subtest("Tes Karakteristik Pribadi", "Pelayanan Publik" to 26, "Jejaring Kerja" to 22),
                    ),
            ),
        )

    /** Topics preselected on "Buat permainan", as in the design. */
    val defaultTopics = listOf("Aljabar", "Logaritma")

    private fun subtest(
        name: String,
        vararg topics: Pair<String, Int>,
    ) = GameSubtest(name = name, topics = topics.map { (topic, stock) -> GameTopic(topic, stock) })

    private fun settings(
        type: GameType,
        subtest: String,
        questionCount: Int,
        durationMinutes: Int,
    ) = GameSettings(
        type = type,
        category = UTBK,
        subtest = subtest,
        topics = emptyList(),
        questionCount = questionCount,
        durationMinutes = durationMinutes,
    )

    /** "ccw e…": c = correct, w = wrong, anything else = empty. */
    private fun outcomes(pattern: String): List<QuestionOutcome> =
        pattern.map {
            when (it) {
                'c' -> QuestionOutcome.Correct
                'w' -> QuestionOutcome.Wrong
                else -> QuestionOutcome.Empty
            }
        }

    private fun standing(
        player: PlayerProfile,
        correct: Int,
        totalSeconds: Int,
    ) = PlayerStanding(
        rank = 0,
        playerId = player.id,
        name = player.name,
        avatarUrl = player.avatarUrl,
        correct = correct,
        totalSeconds = totalSeconds,
        isCurrentUser = player.id == CURRENT_USER_ID,
    )

    private fun finishedRecord(
        code: String,
        settings: GameSettings,
        playedAt: LocalDateTime,
        standings: List<Pair<PlayerProfile, PlayerStanding>>,
        myOutcomes: List<QuestionOutcome>,
    ) = GameRecord(
        code = code,
        settings = settings,
        hostId = CURRENT_USER_ID,
        seats = standings.map { (profile, _) -> Seat(profile, joinedAt = 0L, finishedAt = 0L) },
        createdAt = 0L,
        autoCancelAt = null,
        startedAt = 0L,
        currentUserId = CURRENT_USER_ID,
        playedAt = playedAt,
        fixedStatus = GameStatus.Finished,
        seededResult =
            GameResult(
                code = code,
                settings = settings,
                playedAt = playedAt,
                standings = GameRules.rank(standings.map { it.second }),
                myOutcomes = myOutcomes,
            ),
    )

    private fun ranked(vararg entries: Triple<PlayerProfile, Int, Int>) =
        entries.map { (player, correct, seconds) -> player to standing(player, correct, seconds) }

    /** Games that do not change over time: other people's games and the student's history. */
    private val fixedRecords: List<GameRecord> =
        listOf(
            GameRecord(
                code = DummyCodes.STARTED,
                settings = settings(GameType.Duel, PENALARAN_MATEMATIKA, questionCount = 20, durationMinutes = 15),
                hostId = michael.id,
                seats = listOf(Seat(michael, joinedAt = 0L), Seat(emily, joinedAt = 0L)),
                createdAt = 0L,
                autoCancelAt = null,
                startedAt = 0L,
                fixedStatus = GameStatus.Started,
            ),
            GameRecord(
                code = DummyCodes.FULL,
                settings = settings(GameType.Room, "Penalaran Umum", questionCount = 20, durationMinutes = 20),
                hostId = john.id,
                seats =
                    listOf(john, michael, emily, rizky, sinta, bima, nadia, ardy, gilang, tari)
                        .map { Seat(it, joinedAt = 0L) },
                createdAt = 0L,
                autoCancelAt = null,
                fixedStatus = GameStatus.Waiting,
            ),
            GameRecord(
                code = DummyCodes.INACTIVE,
                settings = settings(GameType.Room, "Penalaran Umum", questionCount = 20, durationMinutes = 30),
                hostId = CURRENT_USER_ID,
                seats = listOf(Seat(me, joinedAt = 0L)),
                createdAt = 0L,
                autoCancelAt = null,
                currentUserId = CURRENT_USER_ID,
                playedAt = LocalDateTime.of(2026, 10, 19, 9, 12),
                fixedStatus = GameStatus.Cancelled,
            ),
            finishedRecord(
                code = DummyCodes.HISTORY_DUEL_WIN,
                settings = settings(GameType.Duel, PENALARAN_MATEMATIKA, questionCount = 20, durationMinutes = 15),
                playedAt = LocalDateTime.of(2026, 10, 22, 19, 20),
                standings = ranked(Triple(me, 16, 782), Triple(ardy, 14, 730)),
                myOutcomes = outcomes("ccwccccccwccccccecwc"),
            ),
            finishedRecord(
                code = DummyCodes.HISTORY_ROOM,
                settings =
                    settings(
                        GameType.Room,
                        "Literasi Bahasa Indonesia",
                        questionCount = 15,
                        durationMinutes = 20,
                    ),
                playedAt = LocalDateTime.of(2026, 10, 21, 20, 5),
                standings =
                    ranked(
                        Triple(nadia, 13, 640),
                        Triple(me, 12, 665),
                        Triple(ardy, 12, 750),
                        Triple(rizky, 10, 800),
                        Triple(sinta, 9, 1_020),
                        Triple(bima, 7, 1_140),
                    ),
                myOutcomes = outcomes("cccwccccecccwcc"),
            ),
            finishedRecord(
                code = DummyCodes.HISTORY_DUEL_DRAW,
                settings = settings(GameType.Duel, "Pengetahuan Kuantitatif", questionCount = 10, durationMinutes = 10),
                playedAt = LocalDateTime.of(2026, 10, 20, 16, 40),
                standings = ranked(Triple(me, 7, 510), Triple(nadia, 7, 510)),
                myOutcomes = outcomes("ccwcccecwc"),
            ),
        )

    private val hostJoinedAgoMillis = 3 * GameRules.MILLIS_PER_MINUTE
    private val ardyJoinedAgoMillis = 2 * GameRules.MILLIS_PER_MINUTE
    private val nadiaJoinedAgoMillis = 20 * GameRules.MILLIS_PER_SECOND
    private val foundRoomScript =
        JoinScript(
            seatId = ardy.id,
            // The design's waiting room shows "Batal otomatis dalam 04:12" right after joining.
            autoCancelInMillis = 252 * GameRules.MILLIS_PER_SECOND,
            hostStartsInMillis = 15 * GameRules.MILLIS_PER_SECOND,
        )

    /** Seeded games, keyed by code. [nowMillis] anchors the live lobby's join times. */
    fun seedRecords(nowMillis: Long): Map<String, GameRecord> =
        (fixedRecords + foundRoom(nowMillis)).associateBy { it.code }

    /**
     * "KTR4QZ": a room hosted by Gilang Permana with Ardy Pratama and Nadia Putri (3 of 10). As in
     * the design's player view, the student who joins takes Ardy Pratama's seat.
     */
    private fun foundRoom(nowMillis: Long) =
        GameRecord(
            code = DummyCodes.FOUND,
            settings = settings(GameType.Room, PENALARAN_MATEMATIKA, questionCount = 20, durationMinutes = 15),
            hostId = gilang.id,
            seats =
                listOf(
                    Seat(gilang, joinedAt = nowMillis - hostJoinedAgoMillis),
                    Seat(ardy, joinedAt = nowMillis - ardyJoinedAgoMillis),
                    Seat(nadia, joinedAt = nowMillis - nadiaJoinedAgoMillis),
                ),
            createdAt = nowMillis - hostJoinedAgoMillis,
            autoCancelAt = null,
            joinScript = foundRoomScript,
        )
}
