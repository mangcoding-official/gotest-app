package com.gotest.app.feature.multiplayer.domain

import com.gotest.app.core.model.Question
import com.gotest.app.core.model.QuestionAnswer
import java.time.LocalDateTime

/** "Duel" is exactly two players; "Room" holds two to ten, host included. */
enum class GameType(
    val minPlayers: Int,
    val maxPlayers: Int,
) {
    Duel(minPlayers = 2, maxPlayers = 2),
    Room(minPlayers = 2, maxPlayers = 10),
}

enum class GameStatus { Waiting, Started, Cancelled, Finished }

enum class CancelReason {
    /** The host pressed "Batalkan Permainan". */
    ByHost,

    /** Nobody started the game within [GameRules.AUTO_CANCEL_MILLIS]. */
    AutoCancel,
}

/** A player's seat state ("MB/Player Row"); empty seats are drawn by the UI, not modelled here. */
enum class PlayerState {
    /** "Pembuat": the player who created the game. */
    Host,

    /** "Bergabung": joined the waiting room and is ready. */
    Joined,

    /** "Mengerjakan": still answering. */
    Playing,

    /** "Selesai": answers submitted. */
    Finished,
}

data class GamePlayer(
    val id: String,
    val name: String,
    /** Coil model for the avatar. */
    val avatarUrl: String?,
    val state: PlayerState,
    val isHost: Boolean,
    val isCurrentUser: Boolean,
    val joinedAtMillis: Long,
    val finishedAtMillis: Long?,
)

/** Everything the host picks on "Buat permainan". */
data class GameSettings(
    val type: GameType,
    val category: String,
    val subtest: String,
    /** Empty means every topic of [subtest]. */
    val topics: List<String>,
    val questionCount: Int,
    val durationMinutes: Int,
)

data class Game(
    /** Six uppercase letters or digits, e.g. "KTR4QZ". */
    val code: String,
    val settings: GameSettings,
    val status: GameStatus,
    val players: List<GamePlayer>,
    val createdAtMillis: Long,
    /** When an unstarted game cancels itself; null for games that are not simulated. */
    val autoCancelAtMillis: Long?,
    val startedAtMillis: Long?,
    val cancelReason: CancelReason?,
) {
    val type: GameType get() = settings.type
    val host: GamePlayer? get() = players.firstOrNull { it.isHost }
    val currentUser: GamePlayer? get() = players.firstOrNull { it.isCurrentUser }
    val isHostedByCurrentUser: Boolean get() = host?.isCurrentUser == true
    val maxPlayers: Int get() = settings.type.maxPlayers
    val seatsLeft: Int get() = (maxPlayers - players.size).coerceAtLeast(0)
    val isFull: Boolean get() = seatsLeft == 0
    val canStart: Boolean get() = status == GameStatus.Waiting && players.size >= settings.type.minPlayers
    val endsAtMillis: Long?
        get() = startedAtMillis?.let { it + settings.durationMinutes * GameRules.MILLIS_PER_MINUTE }

    /** The other player of a duel, from the current user's point of view. */
    val opponent: GamePlayer? get() = if (type == GameType.Duel) players.firstOrNull { !it.isCurrentUser } else null
}

/** Answer to "Gabung dengan kode". */
sealed interface JoinLookup {
    data class Found(
        val game: Game,
    ) : JoinLookup

    data object NotFound : JoinLookup

    data object AlreadyStarted : JoinLookup

    data object Full : JoinLookup

    /** The game was cancelled or has finished. */
    data object Inactive : JoinLookup

    /** The student must finish or leave [activeGame] first. */
    data class InAnotherGame(
        val activeGame: Game,
    ) : JoinLookup
}

data class GameTopic(
    val name: String,
    /** Questions available for this topic in the bank. */
    val questionStock: Int,
)

data class GameSubtest(
    val name: String,
    val topics: List<GameTopic>,
)

data class GameCategory(
    val name: String,
    val subtests: List<GameSubtest>,
)

/** A running game from the current player's side. */
data class GameSession(
    val game: Game,
    val questions: List<Question>,
    val answers: Map<String, QuestionAnswer>,
)

enum class QuestionOutcome { Correct, Wrong, Empty }

/** One line of the final ranking. */
data class PlayerStanding(
    val rank: Int,
    val playerId: String,
    val name: String,
    val avatarUrl: String?,
    val correct: Int,
    val totalSeconds: Int,
    val isCurrentUser: Boolean,
)

data class GameResult(
    val code: String,
    val settings: GameSettings,
    val playedAt: LocalDateTime,
    /** Sorted by rank: most correct first, then the faster total time. */
    val standings: List<PlayerStanding>,
    /** The current user's outcome per question, in question order. */
    val myOutcomes: List<QuestionOutcome>,
) {
    val me: PlayerStanding? get() = standings.firstOrNull { it.isCurrentUser }
    val winner: PlayerStanding? get() = standings.firstOrNull()
    val myCorrect: Int get() = myOutcomes.count { it == QuestionOutcome.Correct }
    val myWrong: Int get() = myOutcomes.count { it == QuestionOutcome.Wrong }
    val myEmpty: Int get() = myOutcomes.count { it == QuestionOutcome.Empty }
}

/** Result chip of a "Riwayat Main Bareng" row. */
sealed interface HistoryOutcome {
    data object Won : HistoryOutcome

    data object Lost : HistoryOutcome

    data object Draw : HistoryOutcome

    data class Ranked(
        val rank: Int,
        val players: Int,
    ) : HistoryOutcome

    data object Cancelled : HistoryOutcome
}

data class GameHistoryItem(
    val code: String,
    val settings: GameSettings,
    val playedAt: LocalDateTime,
    val outcome: HistoryOutcome,
    /** Duel opponent's name; null for rooms. */
    val opponentName: String?,
    val playerCount: Int,
) {
    val hasResult: Boolean get() = outcome != HistoryOutcome.Cancelled
}
