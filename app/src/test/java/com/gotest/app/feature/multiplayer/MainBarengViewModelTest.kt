package com.gotest.app.feature.multiplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.multiplayer.ScriptedMainBareng.Companion.ME
import com.gotest.app.feature.multiplayer.ScriptedMainBareng.Companion.RIVAL
import com.gotest.app.feature.multiplayer.ScriptedMainBareng.Companion.game
import com.gotest.app.feature.multiplayer.ScriptedMainBareng.Companion.review
import com.gotest.app.feature.multiplayer.domain.GameOrganization
import com.gotest.app.feature.multiplayer.domain.GameProblem
import com.gotest.app.feature.multiplayer.domain.GameQuestion
import com.gotest.app.feature.multiplayer.domain.GameState
import com.gotest.app.feature.multiplayer.domain.HubAccess
import com.gotest.app.feature.multiplayer.domain.HubPage
import com.gotest.app.feature.multiplayer.ui.GameScreen
import com.gotest.app.feature.multiplayer.ui.HubDialog
import com.gotest.app.feature.multiplayer.ui.HubFlash
import com.gotest.app.feature.multiplayer.ui.MainBarengEffect
import com.gotest.app.feature.multiplayer.ui.MainBarengViewModel
import com.gotest.app.feature.multiplayer.ui.PlayError
import com.gotest.app.feature.multiplayer.ui.PlayEvent
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.ruang.data.ElapsedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Main Bareng (A2d, web MainBareng/Index): the hub's dialogs, joining by code, the waiting room read every 3
 * seconds only while shown (API-K9), the shared countdown, answering, finishing, and the result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainBarengViewModelTest {
    private val repository = ScriptedMainBareng()

    private fun TestScope.viewModel(
        space: String? = null,
        action: String? = null,
    ): MainBarengViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val args =
            buildMap<String, Any> {
                space?.let { put(MainBarengViewModel.ARG_SPACE, it) }
                action?.let { put(MainBarengViewModel.ARG_ACTION, it) }
            }
        return MainBarengViewModel(SavedStateHandle(args), repository, ElapsedClock { testScheduler.currentTime })
            .also { runCurrent() }
    }

    /** Back into a running game the viewer has not finished: the countdown, then the questions. */
    private fun TestScope.playing(): MainBarengViewModel {
        repository.activeReply = Result.success(game(GameState.STATUS_RUNNING, listOf(ME, RIVAL)))
        repository.questionsReply = Result.success(QUESTIONS)
        val vm = viewModel(action = MainBarengViewModel.ACTION_RESUME)
        assertEquals(GameScreen.Countdown, vm.uiState.value.screen)
        advanceTimeBy(START_MS)
        runCurrent()
        assertEquals(GameScreen.Play, vm.uiState.value.screen)
        return vm
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun creatingNeedsPremiumAndNoOtherGame() =
        runTest {
            repository.hubReply = Result.success(HubPage(HubAccess("expired"), ScriptedMainBareng.OPTIONS))
            val free = viewModel()
            free.openCreate()
            assertEquals(HubDialog.Premium, free.uiState.value.dialog)

            repository.hubReply = Result.success(HubPage(HubAccess("active"), ScriptedMainBareng.OPTIONS))
            repository.activeReply = Result.success(game(GameState.STATUS_WAITING))
            val busy = viewModel()
            busy.openCreate()
            assertEquals(HubDialog.Busy, busy.uiState.value.dialog)

            repository.activeReply = Result.success(null)
            val fromHome = viewModel(action = MainBarengViewModel.ACTION_CREATE)
            assertEquals(HubDialog.Create, fromHome.uiState.value.dialog)
        }

    @Test
    fun theFormSendsItsChoicesAndOpensTheWaitingRoom() =
        runTest {
            val vm = viewModel(space = "SMP1BTG")
            repository.createReply = Result.success(game())
            vm.openCreate()
            vm.updateDraft { it.copy(mode = GameState.MODE_ROOM, questionCount = 25) }
            vm.create()
            runCurrent()

            val (space, settings) = repository.created.single()
            assertEquals("SMP1BTG", space)
            assertEquals(GameState.MODE_ROOM, settings.mode)
            assertEquals(11L, settings.subjectId)
            assertEquals(25, settings.questionCount)
            // 25 questions need at least 13 minutes: 15 is the shortest choice (30 seconds per question).
            assertEquals(15, settings.durationMinutes)
            assertEquals(GameScreen.Lobby, vm.uiState.value.screen)
            assertEquals(HubDialog.None, vm.uiState.value.dialog)
            vm.viewModelScope.cancel()
        }

    @Test
    fun sixLettersLookTheCodeUpAndJoiningOpensTheWaitingRoom() =
        runTest {
            val vm = viewModel()
            repository.lookupReply = Result.success(game(host = false, players = listOf(RIVAL)))
            repository.joinReply = Result.success(game(host = false, players = listOf(RIVAL, ME.copy(isHost = false))))

            vm.openJoin()
            vm.onJoinCode("abc12")
            runCurrent()
            assertTrue(repository.lookups.isEmpty())
            vm.onJoinCode("abc123")
            runCurrent()
            assertEquals(listOf("ABC123"), repository.lookups)
            assertEquals(
                "ABC123",
                vm.uiState.value.join.found
                    ?.code,
            )

            vm.submitJoin()
            runCurrent()
            assertEquals(GameScreen.Lobby, vm.uiState.value.screen)
            assertEquals(HubDialog.None, vm.uiState.value.dialog)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aRefusedJoinSaysWhyAndLeadsAway() =
        runTest {
            val vm = viewModel()
            vm.openJoin()
            vm.onJoinCode("ABC")
            vm.submitJoin()
            assertEquals(GameProblem.ShortCode, vm.uiState.value.join.problem)

            repository.lookupReply = Result.failure(ApiError(422, "Duel ini sudah ada 2 pemain.", "full"))
            vm.onJoinCode("PENUH1")
            runCurrent()
            assertEquals(GameProblem.Server("full", "Duel ini sudah ada 2 pemain."), vm.uiState.value.join.problem)
            assertTrue(vm.uiState.value.join.blocking)
        }

    @Test
    fun theWaitingRoomIsReadEveryThreeSecondsOnlyWhileShown() =
        runTest {
            val vm = viewModel()
            repository.createReply = Result.success(game())
            repository.stateReply = Result.success(game(players = listOf(ME, RIVAL)))
            vm.openCreate()
            vm.create()
            runCurrent()

            advanceTimeBy(10_000)
            assertEquals(0, repository.stateCalls)
            vm.onResume()
            advanceTimeBy(3_100)
            assertEquals(1, repository.stateCalls)
            assertEquals(
                2,
                vm.uiState.value.game
                    ?.players
                    ?.size,
            )
            vm.onPause()
            advanceTimeBy(10_000)
            assertEquals(1, repository.stateCalls)

            repository.stateReply = Result.success(game(GameState.STATUS_RUNNING, listOf(ME, RIVAL)))
            vm.onResume()
            advanceTimeBy(3_100)
            assertEquals(GameScreen.Countdown, vm.uiState.value.screen)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aPlayerTakenOutOfTheGameGoesBackToTheHub() =
        runTest {
            val vm = viewModel()
            repository.joinReply = Result.success(game(host = false, players = listOf(RIVAL, ME.copy(isHost = false))))
            repository.lookupReply = repository.joinReply
            vm.openJoin("ABC123")
            runCurrent()
            vm.submitJoin()
            runCurrent()
            vm.onResume()

            repository.stateReply = Result.failure(ApiError(404, "Data tidak ditemukan.", "not_found"))
            advanceTimeBy(3_100)
            runCurrent()
            assertEquals(GameScreen.Hub, vm.uiState.value.screen)
            assertEquals(HubFlash.Removed, vm.uiState.value.flash)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aGameOfAnotherSpaceOpensThatSpace() =
        runTest {
            val vm = viewModel()
            repository.activeReply =
                Result.success(
                    game(GameState.STATUS_RUNNING, organization = GameOrganization("SMP 1 Bantargedung", "SMP1BTG")),
                )

            vm.resume()
            runCurrent()

            assertEquals(MainBarengEffect.OpenSpace("SMP1BTG"), vm.effects.first())
            assertEquals(GameScreen.Hub, vm.uiState.value.screen)
        }

    @Test
    fun theCountdownOpensTheQuestionsWithTheSavedAnswers() =
        runTest {
            val vm = playing()
            val play = vm.uiState.value.play

            assertEquals(2, play?.questions?.size)
            assertEquals(AnswerValue.Single(2), play?.answers?.get(QUESTIONS[1].id))
            vm.viewModelScope.cancel()
        }

    @Test
    fun answersGoNewestLastAndAFailedOneIsSentAgain() =
        runTest {
            val vm = playing()
            vm.onPlay(PlayEvent.Select(1))
            vm.onPlay(PlayEvent.Select(3))
            runCurrent()
            // One save per question at a time: the newest answer is the last to land.
            assertEquals(AnswerData(selectedOption = 3), repository.answers.last().second)

            var tries = 0
            repository.answerReply = { if (tries++ == 0) ScriptedMainBareng.offline() else Result.success(Unit) }
            vm.onPlay(PlayEvent.GoTo(1))
            vm.onPlay(PlayEvent.Select(0))
            runCurrent()
            assertEquals(
                listOf(2),
                vm.uiState.value.play
                    ?.unsentNumbers,
            )

            advanceTimeBy(4_100)
            runCurrent()
            assertEquals(
                emptyList<Int>(),
                vm.uiState.value.play
                    ?.unsentNumbers,
            )
            vm.viewModelScope.cancel()
        }

    @Test
    fun finishingWaitsForTheOthersOrShowsTheResult() =
        runTest {
            val waiting = playing()
            repository.finishReply =
                Result.success(game(GameState.STATUS_RUNNING, listOf(ME, RIVAL), youFinished = true))
            waiting.onPlay(PlayEvent.Finish)
            runCurrent()
            assertEquals(GameScreen.Waiting, waiting.uiState.value.screen)
            waiting.viewModelScope.cancel()

            val done = playing()
            repository.finishReply =
                Result.success(game(GameState.STATUS_FINISHED, listOf(ME, RIVAL), youFinished = true))
            repository.resultReply = Result.success(review())
            done.onPlay(PlayEvent.Finish)
            runCurrent()
            assertEquals(GameScreen.Result, done.uiState.value.screen)
            assertTrue(done.uiState.value.liveResult)
            assertNull(done.uiState.value.play)
            done.viewModelScope.cancel()
        }

    @Test
    fun theTimeRunningOutFinishesOnItsOwnOnce() =
        runTest {
            val vm = playing()
            repository.finishReply =
                Result.success(game(GameState.STATUS_RUNNING, listOf(ME, RIVAL), youFinished = true))

            advanceTimeBy(GAME_MS)
            runCurrent()

            assertEquals(1, repository.finishCalls)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aFailedFinishWhenTimeIsUpIsSentOnceThenWaitsForSelesai() =
        runTest {
            val vm = playing()
            repository.finishReply = ScriptedMainBareng.offline()

            advanceTimeBy(GAME_MS)
            runCurrent()
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals("Sent once, not on every tick (A2d review 1)", 1, repository.finishCalls)
            assertEquals(
                PlayError.FinishFailed,
                vm.uiState.value.play
                    ?.error,
            )

            repository.finishReply =
                Result.success(game(GameState.STATUS_RUNNING, listOf(ME, RIVAL), youFinished = true))
            vm.onPlay(PlayEvent.Finish)
            runCurrent()
            assertEquals(2, repository.finishCalls)
            assertEquals(GameScreen.Waiting, vm.uiState.value.screen)
            vm.viewModelScope.cancel()
        }

    @Test
    fun anAnswerRefusedForGoodIsNotSentAgain() =
        runTest {
            val vm = playing()
            repository.answerReply = { Result.failure(ApiError(422, "Waktu permainan sudah habis.", "closed")) }

            vm.onPlay(PlayEvent.Select(1))
            runCurrent()
            advanceTimeBy(10_000)
            runCurrent()

            assertEquals(1, repository.answers.size)
            assertEquals(
                emptyList<Int>(),
                vm.uiState.value.play
                    ?.unsentNumbers,
            )
            vm.viewModelScope.cancel()
        }

    @Test
    fun leavingGoesOnceWhilePressedTwice() =
        runTest {
            val vm = playing()
            repository.leaveReply = ScriptedMainBareng.offline()

            vm.onPlay(PlayEvent.Leave)
            vm.onPlay(PlayEvent.Leave)
            runCurrent()

            assertEquals(1, repository.leaveCalls)
            assertEquals(
                PlayError.LeaveFailed,
                vm.uiState.value.play
                    ?.error,
            )
            assertEquals(
                false,
                vm.uiState.value.play
                    ?.leaving,
            )
            vm.viewModelScope.cancel()
        }

    @Test
    fun aGuestWatchesTheResultUntilTheNextGameThenJoinsIt() =
        runTest {
            val guest =
                game(
                    GameState.STATUS_FINISHED,
                    listOf(RIVAL.copy(isHost = true), ME.copy(isHost = false)),
                    host = false,
                )
            repository.activeReply =
                Result.success(guest.copy(status = GameState.STATUS_RUNNING, youFinished = true))
            val vm = viewModel(action = MainBarengViewModel.ACTION_RESUME)
            vm.onResume()
            assertEquals(GameScreen.Waiting, vm.uiState.value.screen)

            repository.stateReply = Result.success(guest)
            repository.resultReply = Result.success(review(guest, outcome = null))
            advanceTimeBy(3_100)
            runCurrent()
            assertEquals(GameScreen.Result, vm.uiState.value.screen)

            repository.stateReply = Result.success(guest.copy(nextGameCode = "NEXT01"))
            advanceTimeBy(3_100)
            runCurrent()
            val reads = repository.stateCalls
            advanceTimeBy(10_000)
            assertEquals("Reading stops once the next game is there", reads, repository.stateCalls)

            repository.playAgainReply = Result.success(guest.copy(code = "NEXT01", status = GameState.STATUS_WAITING))
            vm.playAgain()
            runCurrent()
            assertEquals(GameScreen.Lobby, vm.uiState.value.screen)
            assertEquals(
                "NEXT01",
                vm.uiState.value.game
                    ?.code,
            )
            vm.viewModelScope.cancel()
        }

    @Test
    fun refusalsReadAsTheWebDoes() {
        assertEquals(
            GameProblem.Server("busy", "Kamu masih ikut."),
            GameProblem.of(ApiError(422, "Kamu masih ikut.", "busy")),
        )
        assertEquals(GameProblem.NotFound, GameProblem.of(ApiError(404, "Data tidak ditemukan.", "not_found")))
        assertEquals(
            GameProblem.Invalid("Subtes wajib dipilih."),
            GameProblem.of(
                ApiError(
                    422,
                    "Data tidak valid.",
                    "validation",
                    mapOf("subject_id" to listOf("Subtes wajib dipilih.")),
                ),
            ),
        )
        assertEquals(GameProblem.Connection, GameProblem.of(ApiError(403, "Akses ditolak.", "forbidden")))
        assertEquals(GameProblem.Connection, GameProblem.of(java.io.IOException()))
    }

    private companion object {
        /** Just past the shared start, five seconds after the state was read. */
        const val START_MS = 5_300L

        /** Past the end of a ten-minute game. */
        const val GAME_MS = 10 * 60_000L + 1_000L

        val QUESTIONS =
            listOf(
                GameQuestion(id = 901, order = 1, questionType = "multiple_choice"),
                GameQuestion(id = 902, order = 2, questionType = "multiple_choice", answer = JsonPrimitive(2)),
            )
    }
}
