package com.gotest.app.feature.tryout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.ruang.data.ElapsedClock
import com.gotest.app.feature.tryout.domain.SessionLeaderboard
import com.gotest.app.feature.tryout.domain.TryoutActiveSection
import com.gotest.app.feature.tryout.domain.TryoutAttempt
import com.gotest.app.feature.tryout.domain.TryoutCatalogPage
import com.gotest.app.feature.tryout.domain.TryoutItem
import com.gotest.app.feature.tryout.domain.TryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutResult
import com.gotest.app.feature.tryout.domain.TryoutSectionSummary
import com.gotest.app.feature.tryout.domain.TryoutSessionPage
import com.gotest.app.feature.tryout.ui.runner.TryoutDialog
import com.gotest.app.feature.tryout.ui.runner.TryoutMessage
import com.gotest.app.feature.tryout.ui.runner.TryoutRunnerEffect
import com.gotest.app.feature.tryout.ui.runner.TryoutRunnerEvent
import com.gotest.app.feature.tryout.ui.runner.TryoutRunnerViewModel
import com.gotest.app.feature.tryout.ui.runner.endCancels
import com.gotest.app.feature.tryout.ui.runner.remainingSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** The Try Out runner (A2c, web Tryout/Runner): sections, saving, ragu-ragu, ending (DEC-062 to 067, DEC-090). */
@OptIn(ExperimentalCoroutinesApi::class)
class TryoutRunnerViewModelTest {
    private class ScriptedTryouts : TryoutRepository {
        var attemptReply: Result<TryoutAttempt> = Result.failure(IOException())
        var beginReply: Result<TryoutAttempt> = Result.failure(IOException())
        var answerReply: Result<Unit> = Result.success(Unit)
        var flagReply: Result<Unit> = Result.success(Unit)
        var finishReply: Result<TryoutAttempt> = Result.failure(IOException())
        var submitReply: Result<TryoutAttempt> = Result.failure(IOException())
        val answers = mutableListOf<Pair<Long, AnswerData>>()
        val finished = mutableListOf<Long?>()

        override suspend fun catalog(): Result<TryoutCatalogPage> = Result.failure(IOException())

        override suspend fun start(packageId: Long): Result<TryoutAttempt> = Result.failure(IOException())

        override suspend fun attempt(attemptId: Long): Result<TryoutAttempt> = attemptReply

        override suspend fun begin(attemptId: Long): Result<TryoutAttempt> = beginReply

        override suspend fun answer(
            attemptId: Long,
            itemId: Long,
            answer: AnswerData,
        ): Result<Unit> {
            answers += itemId to answer
            return answerReply
        }

        override suspend fun flag(
            attemptId: Long,
            itemId: Long,
            flagged: Boolean,
        ): Result<Unit> = flagReply

        override suspend fun finishSection(
            attemptId: Long,
            sectionId: Long?,
        ): Result<TryoutAttempt> {
            finished += sectionId
            return finishReply
        }

        override suspend fun submit(attemptId: Long): Result<TryoutAttempt> = submitReply

        override suspend fun result(attemptId: Long): Result<TryoutResult> = Result.failure(IOException())

        override suspend fun session(sessionId: Long): Result<TryoutSessionPage> = Result.failure(IOException())

        override suspend fun register(sessionId: Long): Result<String?> = Result.failure(IOException())

        override suspend fun unregister(sessionId: Long): Result<String?> = Result.failure(IOException())

        override suspend fun startSession(sessionId: Long): Result<Long> = Result.failure(IOException())

        override suspend fun sessionLeaderboard(sessionId: Long): Result<SessionLeaderboard> =
            Result.failure(IOException())
    }

    private val repository = ScriptedTryouts()

    private fun summary(
        id: Long,
        status: String,
    ) = TryoutSectionSummary(id, "Subtes $id", id.toInt(), status, 300, 2)

    /** Before the first subtest: nothing runs, every section pending. */
    private fun notBegun(inSession: Boolean = false) =
        TryoutAttempt(
            id = ATTEMPT,
            status = TryoutAttempt.STATUS_STARTED,
            inSession = inSession,
            serverNow = "2026-11-20T03:00:00Z",
            nextSection = summary(1, TryoutSectionSummary.PENDING),
            sections = listOf(summary(1, TryoutSectionSummary.PENDING), summary(2, TryoutSectionSummary.PENDING)),
        )

    private fun inSection(
        section: Long,
        items: List<TryoutItem> =
            listOf(TryoutItem(section * 10 + 1, displayOrder = 1), TryoutItem(section * 10 + 2, displayOrder = 2)),
    ) = TryoutAttempt(
        id = ATTEMPT,
        status = TryoutAttempt.STATUS_STARTED,
        serverNow = "2026-11-20T03:00:00Z",
        activeSection =
            TryoutActiveSection(
                section,
                "Subtes $section",
                section.toInt(),
                deadlineAt = "2026-11-20T03:05:00Z",
                items = items,
            ),
        sections =
            listOf(
                summary(1, if (section == 1L) TryoutSectionSummary.STARTED else "completed"),
                summary(2, if (section == 2L) TryoutSectionSummary.STARTED else TryoutSectionSummary.PENDING),
            ),
    )

    private fun TestScope.viewModel(first: TryoutAttempt): TryoutRunnerViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        repository.attemptReply = Result.success(first)
        return TryoutRunnerViewModel(
            SavedStateHandle(mapOf(TryoutRunnerViewModel.ARG_ATTEMPT to ATTEMPT)),
            repository,
            ElapsedClock { testScheduler.currentTime },
        ).also { runCurrent() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun beginStartsTheFirstSectionAndItsClock() =
        runTest {
            val vm = viewModel(notBegun())
            assertTrue(vm.uiState.value.endCancels())
            assertEquals(2, vm.uiState.value.remainingSections())

            repository.beginReply = Result.success(inSection(1))
            vm.onEvent(TryoutRunnerEvent.Begin)
            runCurrent()

            assertEquals(
                1L,
                vm.uiState.value.section
                    ?.id,
            )
            assertEquals(
                11L,
                vm.uiState.value.activeItem
                    ?.id,
            )
            assertEquals(300L, vm.uiState.value.secondsLeft)
            assertFalse(vm.uiState.value.endCancels())
            vm.viewModelScope.cancel()
        }

    @Test
    fun aSessionTryOutIsNeverCancelledByEndingEarly() =
        runTest {
            val vm = viewModel(notBegun(inSession = true))

            assertFalse(vm.uiState.value.endCancels())
            vm.viewModelScope.cancel()
        }

    @Test
    fun anAnswerIsSavedByItsItemAndAWaitingOneBlocksMovingOn() =
        runTest {
            val vm = viewModel(inSection(1))
            repository.attemptReply = Result.success(inSection(1))

            vm.onEvent(TryoutRunnerEvent.SelectOption(3))
            runCurrent()
            assertEquals(listOf(11L to AnswerData(selectedOption = 3)), repository.answers)
            assertFalse(vm.uiState.value.hasUnsaved)

            repository.answerReply = Result.failure(IOException())
            vm.onEvent(TryoutRunnerEvent.SelectOption(1))
            runCurrent()
            assertEquals(TryoutMessage.NotSaved(offline = true, reason = null), vm.uiState.value.message)
            assertTrue(vm.uiState.value.hasUnsaved)

            vm.onEvent(TryoutRunnerEvent.Next)
            runCurrent()
            assertEquals(TryoutMessage.KeepAnswering, vm.uiState.value.message)
            assertEquals(
                11L,
                vm.uiState.value.activeItem
                    ?.id,
            )
            vm.viewModelScope.cancel()
        }

    @Test
    fun aRefusedFlagTurnsBackAndSaysWhy() =
        runTest {
            val vm = viewModel(inSection(1))

            vm.onEvent(TryoutRunnerEvent.ToggleFlag)
            assertEquals(true, vm.uiState.value.flags[11L])
            repository.flagReply = Result.failure(ApiError(422, "Subtes ini sudah selesai.", "validation"))
            runCurrent()

            assertEquals(false, vm.uiState.value.flags[11L])
            assertEquals(TryoutMessage.Server("Subtes ini sudah selesai."), vm.uiState.value.message)
            vm.viewModelScope.cancel()
        }

    @Test
    fun finishingASectionMovesToTheNextOneFromTheTop() =
        runTest {
            val vm = viewModel(inSection(1))
            vm.onEvent(TryoutRunnerEvent.GoTo(12))
            runCurrent()
            assertEquals(
                12L,
                vm.uiState.value.activeItem
                    ?.id,
            )

            repository.finishReply = Result.success(inSection(2))
            vm.onEvent(TryoutRunnerEvent.ShowDialog(TryoutDialog.Action))
            vm.onEvent(TryoutRunnerEvent.ConfirmAction)
            runCurrent()

            assertEquals(listOf<Long?>(1), repository.finished)
            assertEquals(
                2L,
                vm.uiState.value.section
                    ?.id,
            )
            assertEquals(
                21L,
                vm.uiState.value.activeItem
                    ?.id,
            )
            assertEquals(TryoutDialog.None, vm.uiState.value.dialog)
            assertTrue(vm.uiState.value.isLastSection)
            vm.viewModelScope.cancel()
        }

    @Test
    fun endingBeforeTheFirstSectionLeavesAndEndingLaterOpensTheResult() =
        runTest {
            val early = viewModel(notBegun())
            repository.submitReply = Result.success(notBegun().copy(status = TryoutAttempt.STATUS_CANCELLED))
            early.onEvent(TryoutRunnerEvent.ConfirmEnd)
            runCurrent()
            assertEquals(TryoutRunnerEffect.Leave, early.effects.first())
            early.viewModelScope.cancel()

            val later = viewModel(inSection(2))
            repository.submitReply = Result.success(inSection(2).copy(status = "submitted"))
            later.onEvent(TryoutRunnerEvent.ConfirmEnd)
            runCurrent()
            assertEquals(TryoutRunnerEffect.OpenResult, later.effects.first())
            later.viewModelScope.cancel()
        }

    @Test
    fun aSectionThatEndedDropsTheAnswersStillWaitingForIt() =
        runTest {
            val vm = viewModel(inSection(1))
            repository.answerReply = Result.failure(IOException())
            vm.onEvent(TryoutRunnerEvent.SelectOption(0))
            runCurrent()
            assertTrue(vm.uiState.value.hasUnsaved)

            // The server moved on to section 2 by itself (time up): the waiting answer can never be saved.
            repository.beginReply = Result.success(inSection(2))
            vm.onEvent(TryoutRunnerEvent.Begin)
            runCurrent()

            assertFalse(vm.uiState.value.hasUnsaved)
            assertEquals(TryoutMessage.StaleDrafts, vm.uiState.value.message)
            vm.viewModelScope.cancel()
        }

    private companion object {
        const val ATTEMPT = 1401L
    }
}
