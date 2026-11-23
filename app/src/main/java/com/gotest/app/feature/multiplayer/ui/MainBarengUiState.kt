package com.gotest.app.feature.multiplayer.ui

import androidx.compose.runtime.Immutable
import com.gotest.app.feature.multiplayer.domain.GameHistoryItem
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameResult
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.HubPage
import com.gotest.app.feature.multiplayer.domain.isAnswered
import com.gotest.app.feature.multiplayer.ui.hub.CreateGameDraft
import com.gotest.app.feature.practice.domain.AnswerValue

/** The steps of the web's MainBareng page, one at a time. */
enum class GameScreen { Hub, Lobby, Countdown, Play, Waiting, Result }

enum class HubDialog { None, Create, Join, Premium, Busy }

/** The status line above the hub (web `flash`). */
enum class HubFlash { HubFailed, Removed, LeftLobby, QuestionsFailed, LeftGame, ResultLater }

/** "Gabung dengan kode": the code, the game it found, and why it cannot be joined. */
@Immutable
data class JoinForm(
    val code: String = "",
    val found: GameState? = null,
    val problem: GameProblem? = null,
    val busy: Boolean = false,
) {
    /** Web `blocking`: the game cannot be joined at all; the button leads away instead. */
    val blocking: Boolean get() = problem?.kind in BLOCKING

    private companion object {
        val BLOCKING = setOf("started", "full", "closed", GameProblem.BUSY, "not_member")
    }
}

@Immutable
data class CreateStatus(
    val submitting: Boolean = false,
    val problem: GameProblem? = null,
)

enum class PlayModal { None, Finish, Leave }

/** "Belum terkirim" above the question: finishing or leaving failed for lack of a connection. */
enum class PlayError { FinishFailed, LeaveFailed }

/** Answering (web PlayScreen): the questions in this player's order and what is still on its way. */
@Immutable
data class PlayState(
    val questions: List<GameQuestion>,
    val index: Int = 0,
    /** By question id: what the student picked, saved or not. */
    val answers: Map<Long, AnswerValue> = emptyMap(),
    /** Questions whose newest answer has not reached the server. */
    val unsent: Set<Long> = emptySet(),
    val modal: PlayModal = PlayModal.None,
    val finishing: Boolean = false,
    val error: PlayError? = null,
) {
    val question: GameQuestion? get() = questions.getOrNull(index)
    val answeredCount: Int get() = questions.count { it.isAnswered(answers[it.id]) }
    val unansweredCount: Int get() = questions.size - answeredCount

    /** The numbers (1-based, in this player's order) of the questions not saved yet. */
    val unsentNumbers: List<Int>
        get() = questions.mapIndexedNotNull { position, item -> (position + 1).takeIf { item.id in unsent } }
}

@Immutable
data class MainBarengUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val hub: HubPage? = null,
    val screen: GameScreen = GameScreen.Hub,
    val dialog: HubDialog = HubDialog.None,
    val active: GameState? = null,
    val history: List<GameHistoryItem> = emptyList(),
    val game: GameState? = null,
    val result: GameResult? = null,
    /** A result reached by playing is watched for the next game; one opened from the history is not. */
    val liveResult: Boolean = false,
    val busy: Boolean = false,
    /** Shown by the hub, the waiting room, and the result (web `error`). */
    val error: GameProblem? = null,
    val flash: HubFlash? = null,
    val join: JoinForm = JoinForm(),
    val create: CreateStatus = CreateStatus(),
    /** The choices of "Buat permainan", kept while the dialog closes and opens again (web modal state). */
    val draft: CreateGameDraft = CreateGameDraft(),
    /** The countdown of the step on screen: the waiting room, the start, or the game. */
    val secondsLeft: Long? = null,
    val play: PlayState? = null,
) {
    val canCreate: Boolean get() = hub?.access?.active == true
    val organizationName: String? get() = hub?.organization?.name
    val timeUp: Boolean get() = secondsLeft == 0L
}

sealed interface MainBarengEffect {
    /** A game of another space opens on that space's page (web `spaceUrl(there, 'lanjut')`). */
    data class OpenSpace(
        val space: String?,
    ) : MainBarengEffect
}
