package com.gotest.app.feature.multiplayer.domain

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.practice.domain.QuestionContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * API module E, Main Bareng (API-CONTRACTS "API Android modul E"): the web's MainBarengController JSON as it is,
 * `camelCase` keys, plus the hub page of MainBarengHubData. Every key named by @SerialName (T-026 required 9).
 */

/** `GET /main-bareng` and `GET /ruang/{code}/main-bareng` (props of web MainBareng/Index). */
@Immutable
@Serializable
data class HubPage(
    @SerialName("access") val access: HubAccess = HubAccess(),
    @SerialName("options") val options: List<OptionCategory> = emptyList(),
    /** The Mitra whose space this hub is; null for Gotest.id. */
    @SerialName("organization") val organization: SpaceOrganization? = null,
)

/** Only `status` is read: Premium (or a paid Mitra) creates games (DEC-085, DEC-128). */
@Serializable
data class HubAccess(
    @SerialName("status") val status: String = "expired",
) {
    val active: Boolean get() = status == "active"
}

@Serializable
data class SpaceOrganization(
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    @SerialName("role") val role: String? = null,
)

@Immutable
@Serializable
data class OptionCategory(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("subjects") val subjects: List<OptionSubject> = emptyList(),
)

@Immutable
@Serializable
data class OptionSubject(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("topics") val topics: List<OptionTopic> = emptyList(),
)

@Serializable
data class OptionTopic(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("questionCount") val questionCount: Int = 0,
)

/** The organization a game belongs to (`{name, code}`); null in the Gotest.id space. */
@Serializable
data class GameOrganization(
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
)

@Immutable
@Serializable
data class GamePlayer(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("isHost") val isHost: Boolean = false,
    @SerialName("isYou") val isYou: Boolean = false,
    @SerialName("finished") val finished: Boolean = false,
    @SerialName("joinedAt") val joinedAt: String? = null,
    @SerialName("finishedAt") val finishedAt: String? = null,
)

/** `{game}` of every game route (web `GameState`). */
@Immutable
@Serializable
data class GameState(
    @SerialName("code") val code: String,
    @SerialName("mode") val mode: String = MODE_DUEL,
    @SerialName("status") val status: String = STATUS_WAITING,
    /** `host`, `expired`, or `removed` (the host left the Mitra). */
    @SerialName("cancelReason") val cancelReason: String? = null,
    @SerialName("organization") val organization: GameOrganization? = null,
    @SerialName("categoryName") val categoryName: String = "",
    @SerialName("subjectName") val subjectName: String = "",
    @SerialName("topicNames") val topicNames: List<String> = emptyList(),
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    @SerialName("maxPlayers") val maxPlayers: Int = 2,
    @SerialName("hostName") val hostName: String = "",
    @SerialName("players") val players: List<GamePlayer> = emptyList(),
    @SerialName("youAreHost") val youAreHost: Boolean = false,
    @SerialName("youFinished") val youFinished: Boolean = false,
    @SerialName("lobbyExpiresAt") val lobbyExpiresAt: String? = null,
    @SerialName("startedAt") val startedAt: String? = null,
    @SerialName("endsAt") val endsAt: String? = null,
    @SerialName("closedAt") val closedAt: String? = null,
    @SerialName("serverNow") val serverNow: String? = null,
    /** Set when the host started a new game with the same settings ("Gabung Lagi"). */
    @SerialName("nextGameCode") val nextGameCode: String? = null,
) {
    val isDuel: Boolean get() = mode == MODE_DUEL
    val waiting: Boolean get() = status == STATUS_WAITING
    val running: Boolean get() = status == STATUS_RUNNING
    val finished: Boolean get() = status == STATUS_FINISHED
    val cancelled: Boolean get() = status == STATUS_CANCELLED

    /** Web Lobby `canStart`: a duel needs exactly two, a room at least two. */
    val canStart: Boolean get() = if (isDuel) players.size == 2 else players.size >= 2

    companion object {
        const val MODE_DUEL = "duel"
        const val MODE_ROOM = "room"
        const val STATUS_WAITING = "waiting"
        const val STATUS_RUNNING = "running"
        const val STATUS_FINISHED = "finished"
        const val STATUS_CANCELLED = "cancelled"
        const val CANCEL_EXPIRED = "expired"
        const val CANCEL_REMOVED = "removed"
    }
}

@Serializable
data class GameEnvelope(
    @SerialName("game") val game: GameState,
)

@Serializable
data class ActiveGameEnvelope(
    @SerialName("game") val game: GameState? = null,
)

/** One row of "Riwayat Main Bareng", seen from the viewer. */
@Immutable
@Serializable
data class GameHistoryItem(
    @SerialName("code") val code: String,
    @SerialName("mode") val mode: String = GameState.MODE_DUEL,
    /** `finished` or `cancelled`. */
    @SerialName("status") val status: String = GameState.STATUS_FINISHED,
    @SerialName("subjectName") val subjectName: String = "",
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("durationMinutes") val durationMinutes: Int = 0,
    @SerialName("playerCount") val playerCount: Int = 0,
    @SerialName("opponentName") val opponentName: String? = null,
    /** Duel only: `win`, `lose`, `draw`. */
    @SerialName("outcome") val outcome: String? = null,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("closedAt") val closedAt: String? = null,
) {
    val finished: Boolean get() = status == GameState.STATUS_FINISHED
}

@Serializable
data class HistoryEnvelope(
    @SerialName("games") val games: List<GameHistoryItem> = emptyList(),
)

/** One question for this player, in this player's order, without a key. */
@Immutable
@Serializable
data class GameQuestion(
    @SerialName("id") val id: Long,
    @SerialName("order") val order: Int = 0,
    @SerialName("questionType") val questionType: String? = null,
    @SerialName("stimulusId") val stimulusId: Long? = null,
    @SerialName("content") val content: QuestionContent = QuestionContent(),
    /** The saved answer: an option index, a list of indexes, or a label per statement (null entries); or null. */
    @SerialName("answer") val answer: JsonElement? = null,
)

@Serializable
data class QuestionsEnvelope(
    @SerialName("questions") val questions: List<GameQuestion> = emptyList(),
)

@Serializable
data class ResultRow(
    @SerialName("rank") val rank: Int,
    @SerialName("playerId") val playerId: Long,
    @SerialName("name") val name: String,
    @SerialName("isYou") val isYou: Boolean = false,
    @SerialName("correct") val correct: Int = 0,
    @SerialName("wrong") val wrong: Int = 0,
    @SerialName("empty") val empty: Int = 0,
    @SerialName("durationSeconds") val durationSeconds: Int = 0,
)

/** `correct`, `wrong`, or `empty`. */
@Serializable
data class ResultItem(
    @SerialName("order") val order: Int,
    @SerialName("status") val status: String,
)

@Immutable
@Serializable
data class GameResult(
    @SerialName("rows") val rows: List<ResultRow> = emptyList(),
    /** Duel only, seen from the viewer: `win`, `lose`, `draw`. */
    @SerialName("outcome") val outcome: String? = null,
    @SerialName("items") val items: List<ResultItem> = emptyList(),
) {
    val you: ResultRow? get() = rows.firstOrNull { it.isYou }

    companion object {
        const val WIN = "win"
        const val LOSE = "lose"
        const val DRAW = "draw"
        const val CORRECT = "correct"
        const val WRONG = "wrong"
    }
}

/** `GET /main-bareng/{code}/hasil`: the game and its result. */
@Serializable
data class GameReview(
    @SerialName("game") val game: GameState,
    @SerialName("result") val result: GameResult,
)

/** `POST /main-bareng` and `POST /ruang/{code}/main-bareng`. */
@Serializable
data class GameSettings(
    @SerialName("mode") val mode: String,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("topic_ids") val topicIds: List<Long> = emptyList(),
    @SerialName("question_count") val questionCount: Int,
    @SerialName("duration_minutes") val durationMinutes: Int,
)
