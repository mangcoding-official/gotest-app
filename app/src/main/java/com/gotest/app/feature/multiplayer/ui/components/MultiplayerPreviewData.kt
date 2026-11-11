package com.gotest.app.feature.multiplayer.ui.components

import com.gotest.app.R
import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionOption
import com.gotest.app.core.model.QuestionType
import com.gotest.app.feature.multiplayer.domain.CancelReason
import com.gotest.app.feature.multiplayer.domain.Game
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GamePlayer
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameRules
import com.gotest.app.feature.multiplayer.domain.GameSettings
import com.gotest.app.feature.multiplayer.domain.GameStatus
import com.gotest.app.feature.multiplayer.domain.GameType
import com.gotest.app.feature.multiplayer.domain.HistoryOutcome
import com.gotest.app.feature.multiplayer.domain.PlayerStanding
import com.gotest.app.feature.multiplayer.domain.PlayerState
import com.gotest.app.feature.multiplayer.domain.QuestionOutcome
import java.time.LocalDateTime

/** Sample data for @Preview functions only; mirrors the design frames. */
internal object MultiplayerPreviewData {
    const val NOW = 1_800_000_000_000L
    private const val MINUTE = GameRules.MILLIS_PER_MINUTE
    private const val CODE = "KTR4QZ"
    private const val CREATED_AGO = 48_000L
    private const val AUTO_CANCEL_IN = 252_000L
    private const val CANCELLED_AGO = 10 * 24 * 60 * MINUTE

    // Previews show the bundled avatars through their resource ids.
    private val gilangAvatar = R.drawable.avatar_gilang
    private val ardyAvatar = R.drawable.avatar_ardy
    private val nadiaAvatar = R.drawable.avatar_nadia
    private val rizkyAvatar = R.drawable.avatar_rizky

    private fun player(
        id: String,
        name: String,
        avatar: Int,
        state: PlayerState,
        isCurrentUser: Boolean = false,
        joinedAgo: Long = 0L,
        finishedAgo: Long? = null,
    ) = GamePlayer(
        id = id,
        name = name,
        avatarUrl = avatarUri(avatar),
        state = state,
        isHost = state == PlayerState.Host || id == "gilang",
        isCurrentUser = isCurrentUser,
        joinedAtMillis = NOW - joinedAgo,
        finishedAtMillis = finishedAgo?.let { NOW - it },
    )

    private fun settings(type: GameType) =
        GameSettings(
            type = type,
            category = "UTBK-SNBT",
            subtest = "Penalaran Matematika",
            topics = listOf("Aljabar", "Logaritma"),
            questionCount = 20,
            durationMinutes = 15,
        )

    private fun lobby(
        type: GameType,
        players: List<GamePlayer>,
        status: GameStatus = GameStatus.Waiting,
    ) = Game(
        code = CODE,
        settings = settings(type),
        status = status,
        players = players,
        createdAtMillis = NOW - CREATED_AGO,
        autoCancelAtMillis = NOW + AUTO_CANCEL_IN,
        startedAtMillis = null,
        cancelReason = null,
    )

    /** "KTR4QZ" as a visitor sees it (frame 16162). */
    val roomLobby: Game =
        lobby(
            GameType.Room,
            listOf(
                player("gilang", "Gilang Permana", gilangAvatar, PlayerState.Host, joinedAgo = 3 * MINUTE),
                player("ardy", "Ardy Pratama", ardyAvatar, PlayerState.Joined, joinedAgo = 2 * MINUTE),
                player("nadia", "Nadia Putri", nadiaAvatar, PlayerState.Joined, joinedAgo = 20_000L),
            ),
        )

    /** Frame 14849: the student hosts the room. */
    val hostRoomLobby: Game =
        roomLobby.copy(
            players = roomLobby.players.map { it.copy(isCurrentUser = it.id == "gilang") },
        )

    /** Frame 15103: a duel waiting for the opponent. */
    val hostDuelLobby: Game = lobby(GameType.Duel, listOf(hostRoomLobby.players.first()))

    /** Frame 15329: the student joined as Ardy Pratama. */
    val playerRoomLobby: Game =
        roomLobby.copy(players = roomLobby.players.map { it.copy(isCurrentUser = it.id == "ardy") })

    /** Frame 19525: two of four players finished. */
    val playingGame: Game =
        lobby(
            GameType.Room,
            listOf(
                player("gilang", "Gilang Permana", gilangAvatar, PlayerState.Finished, true, 0L, 2 * MINUTE),
                player("ardy", "Ardy Pratama", ardyAvatar, PlayerState.Finished, finishedAgo = MINUTE),
                player("nadia", "Nadia Putri", nadiaAvatar, PlayerState.Playing),
                player("rizky", "Rizky Ramadhan", rizkyAvatar, PlayerState.Playing),
            ),
            status = GameStatus.Started,
        )

    private val pmSettings = settings(GameType.Duel).copy(topics = emptyList())

    /** "Riwayat Main Bareng" (frames 12376 and 23813), newest first. */
    val history: List<GameHistoryItem> =
        listOf(
            GameHistoryItem(
                code = "DUEL22",
                settings = pmSettings,
                playedAt = LocalDateTime.of(2026, 10, 22, 19, 20),
                outcome = HistoryOutcome.Won,
                opponentName = "Ardy Pratama",
                playerCount = 2,
            ),
            GameHistoryItem(
                code = "ROOM21",
                settings =
                    pmSettings.copy(
                        type = GameType.Room,
                        subtest = "Literasi Bahasa Indonesia",
                        questionCount = 15,
                        durationMinutes = 20,
                    ),
                playedAt = LocalDateTime.of(2026, 10, 21, 20, 5),
                outcome = HistoryOutcome.Ranked(rank = 2, players = 6),
                opponentName = null,
                playerCount = 6,
            ),
            GameHistoryItem(
                code = "SERI20",
                settings =
                    pmSettings.copy(
                        subtest = "Pengetahuan Kuantitatif",
                        questionCount = 10,
                        durationMinutes = 10,
                    ),
                playedAt = LocalDateTime.of(2026, 10, 20, 16, 40),
                outcome = HistoryOutcome.Draw,
                opponentName = "Nadia Putri",
                playerCount = 2,
            ),
            GameHistoryItem(
                code = "QX7P2M",
                settings =
                    pmSettings.copy(type = GameType.Room, subtest = "Penalaran Umum", durationMinutes = 30),
                playedAt = LocalDateTime.of(2026, 10, 19, 9, 12),
                outcome = HistoryOutcome.Cancelled,
                opponentName = null,
                playerCount = 1,
            ),
            GameHistoryItem(
                code = "KLH17R",
                settings = pmSettings.copy(subtest = "Literasi Bahasa Inggris", questionCount = 15),
                playedAt = LocalDateTime.of(2026, 10, 17, 21, 30),
                outcome = HistoryOutcome.Lost,
                opponentName = "Rizky Hidayat",
                playerCount = 2,
            ),
        )

    /** "QX7P2M": the student's room that cancelled itself with nobody else in it (frame 23999). */
    val cancelledRoom: Game =
        Game(
            code = "QX7P2M",
            settings = pmSettings.copy(type = GameType.Room, subtest = "Penalaran Umum", durationMinutes = 30),
            status = GameStatus.Cancelled,
            players = listOf(hostRoomLobby.players.first()),
            createdAtMillis = NOW - CANCELLED_AGO,
            autoCancelAtMillis = NOW - CANCELLED_AGO + GameRules.AUTO_CANCEL_MILLIS,
            startedAtMillis = null,
            cancelReason = CancelReason.AutoCancel,
            cancelledAtMillis = NOW - CANCELLED_AGO + GameRules.AUTO_CANCEL_MILLIS,
        )

    /** Nadia Putri's room that she cancelled while the student and Rizky waited. */
    val hostCancelledRoom: Game =
        cancelledRoom.copy(
            code = "BTL14N",
            players =
                listOf(
                    player("nadia", "Nadia Putri", nadiaAvatar, PlayerState.Host),
                    player("me", "Gilang Permana", gilangAvatar, PlayerState.Joined, isCurrentUser = true),
                    player("rizky", "Rizky Ramadhan", rizkyAvatar, PlayerState.Joined),
                ),
            cancelReason = CancelReason.ByHost,
            cancelledAtMillis = NOW - CANCELLED_AGO + 3 * MINUTE,
        )

    /** The design's question 4 (frame 19024), repeated for a 20-question game. */
    val questions: List<Question> =
        List(20) { index ->
            Question(
                id = "q${index + 1}",
                number = index + 1,
                topic = "Logaritma",
                type = QuestionType.MultipleChoice,
                stimulusTitle = "Rute Angkutan Pagi di Kampung Melati",
                stimulusBody =
                    "Dalam pemantauan selama satu pekan, rute A mengangkut 44 penumpang per hari, rute B 39 " +
                        "penumpang, dan rute C 30 penumpang. Setiap kendaraan berkapasitas 94 penumpang. " +
                        "Pengelola akan menambah satu perjalanan pada rute yang paling sering penuh.",
                imageUrl = null,
                prompt =
                    "Berdasarkan catatan rute angkutan pagi di Kampung Melati, kesimpulan yang paling kuat adalah ....",
                instruction = null,
                options =
                    listOf(
                        QuestionOption(
                            "A",
                            "Rute A perlu dipantau lebih dulu karena jumlah penumpangnya paling tinggi.",
                        ),
                        QuestionOption("B", "Rute dengan jumlah penumpang paling rendah pasti tidak diperlukan lagi."),
                        QuestionOption(
                            "C",
                            "Semua rute harus diberi kendaraan dengan kapasitas yang sama tanpa melihat data.",
                        ),
                        QuestionOption(
                            "D",
                            "Kapasitas kendaraan tidak berkaitan dengan keputusan menambah perjalanan.",
                        ),
                    ),
                statements = emptyList(),
                correctKeys = setOf("A"),
                correctStatements = emptyMap(),
                explanation = "",
            )
        }

    /** 17 of 20 answered with "C", as in frames 19024 and 19162. */
    val previewAnswers: Map<String, QuestionAnswer> =
        questions.take(17).associate { it.id to QuestionAnswer(selectedKeys = setOf("C")) }

    private fun standing(
        rank: Int,
        name: String,
        avatar: Int,
        correct: Int,
        seconds: Int,
        isMe: Boolean = false,
    ) = PlayerStanding(rank, name, name, avatarUri(avatar), correct, seconds, isMe)

    private fun avatarUri(resId: Int): String = "android.resource://com.gotest.app/$resId"

    private fun outcomes(pattern: String): List<QuestionOutcome> =
        pattern.map {
            when (it) {
                'c' -> QuestionOutcome.Correct
                'w' -> QuestionOutcome.Wrong
                else -> QuestionOutcome.Empty
            }
        }

    /** Frame 19646. */
    val roomResult: GameResult =
        GameResult(
            code = CODE,
            settings = settings(GameType.Room),
            playedAt = LocalDateTime.of(2026, 10, 22, 20, 15),
            standings =
                listOf(
                    standing(1, "Ardy Pratama", ardyAvatar, 18, 761),
                    standing(2, "Nadia Putri", nadiaAvatar, 16, 800),
                    standing(3, "Gilang Permana", gilangAvatar, 14, 845, isMe = true),
                    standing(4, "Rizky Ramadhan", rizkyAvatar, 14, 888),
                    standing(5, "Sinta Lestari", R.drawable.avatar_sinta, 11, 900),
                    standing(6, "Bima Saputra", R.drawable.avatar_bima, 9, 900),
                ),
            myOutcomes = outcomes("ccwcccsccwcccwccscwc"),
        )

    /** Frame 19882, kept consistent: 16 correct beats 14. */
    val duelResult: GameResult =
        GameResult(
            code = CODE,
            settings = settings(GameType.Duel),
            playedAt = LocalDateTime.of(2026, 10, 22, 19, 20),
            standings =
                listOf(
                    standing(1, "Gilang Permana", gilangAvatar, 16, 782, isMe = true),
                    standing(2, "Ardy Pratama", ardyAvatar, 14, 730),
                ),
            myOutcomes = outcomes("ccwccccccwccccccscwc"),
        )
}
