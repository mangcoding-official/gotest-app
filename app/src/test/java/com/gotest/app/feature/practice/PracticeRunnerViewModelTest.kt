package com.gotest.app.feature.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.ActiveQuestion
import com.gotest.app.feature.practice.domain.AnswerData
import com.gotest.app.feature.practice.domain.AnswerValue
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeItem
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerEffect
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerEvent
import com.gotest.app.feature.practice.ui.runner.PracticeRunnerViewModel
import com.gotest.app.feature.practice.ui.runner.RunnerDialog
import com.gotest.app.feature.practice.ui.runner.RunnerMessage
import com.gotest.app.feature.ruang.data.ElapsedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Latihan runner (A2c, web Practice/Runner): saving each answer in order, refusals, linear order, finishing. */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeRunnerViewModelTest {
    private val repository =
        ScriptedPracticeRepository().apply {
            activateReply = { itemId -> Result.success(ActiveQuestion(itemId, questionId = 100 + itemId)) }
        }

    private fun attempt(
        status: String = PracticeAttempt.STATUS_STARTED,
        navigation: String = "free",
        activeItemId: Long = 1,
        multipleSecond: Boolean = false,
    ) = PracticeAttempt(
        id = ATTEMPT,
        status = status,
        serverNow = "2026-11-20T03:00:00Z",
        activeItemId = activeItemId,
        navigation = navigation,
        items =
            listOf(
                PracticeItem(1, 101, 1, questionType = "multiple_choice"),
                PracticeItem(2, 102, 2, questionType = if (multipleSecond) "multiple_select" else "multiple_choice"),
            ),
    )

    private fun TestScope.viewModel(first: PracticeAttempt = attempt()): PracticeRunnerViewModel =
        viewModelReading(Result.success(first))

    private fun TestScope.viewModelReading(first: Result<PracticeAttempt>): PracticeRunnerViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        repository.attemptReply = first
        return PracticeRunnerViewModel(
            SavedStateHandle(mapOf(PracticeRunnerViewModel.ARG_ATTEMPT to ATTEMPT)),
            repository,
            ElapsedClock { testScheduler.currentTime },
        ).also { runCurrent() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theActiveQuestionOpensAndAnAnswerIsSavedRightAway() =
        runTest {
            val vm = viewModel()
            assertEquals(listOf(1L), repository.activated)
            assertTrue(vm.uiState.value.questionReady)

            vm.onEvent(PracticeRunnerEvent.SelectOption(2))
            assertEquals(AnswerValue.Single(2), vm.uiState.value.activeAnswer)
            runCurrent()

            assertEquals(listOf(101L to AnswerData(selectedOption = 2)), repository.answers)
            assertFalse(vm.uiState.value.isSaving)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aMultipleSelectAnswerSendsEveryPickedOption() =
        runTest {
            val vm = viewModel(attempt(activeItemId = 2, multipleSecond = true))

            vm.onEvent(PracticeRunnerEvent.ToggleOption(0))
            vm.onEvent(PracticeRunnerEvent.ToggleOption(3))
            runCurrent()
            vm.onEvent(PracticeRunnerEvent.ToggleOption(0))
            runCurrent()

            assertEquals(AnswerData(selectedOptions = listOf(3)), repository.answers.last().second)
            vm.viewModelScope.cancel()
        }

    @Test
    fun anAnswerRefusedForGoodSaysWhichQuestionAndWhy() =
        runTest {
            val vm = viewModel()
            repository.answerReply = Result.failure(ApiError(422, "Waktu untuk soal ini telah habis.", "validation"))

            vm.onEvent(PracticeRunnerEvent.SelectOption(1))
            runCurrent()

            assertEquals(RunnerMessage.Refused(1, "Waktu untuk soal ini telah habis."), vm.uiState.value.message)
            vm.viewModelScope.cancel()
        }

    @Test
    fun withoutAConnectionTheAnswerWaitsAndSubmitDoesNotGo() =
        runTest {
            val vm = viewModel()
            repository.answerReply = ScriptedPracticeRepository.offline()

            vm.onEvent(PracticeRunnerEvent.SelectOption(0))
            runCurrent()
            assertEquals(RunnerMessage.Offline, vm.uiState.value.message)

            vm.onEvent(PracticeRunnerEvent.ConfirmSubmit)
            runCurrent()
            assertEquals(0, repository.submitCalls)
            assertFalse(vm.uiState.value.isSubmitting)

            // Back online: the waiting answer goes first, then the submit.
            repository.answerReply = Result.success(Unit)
            repository.submitReply = Result.success(attempt(status = "submitted"))
            vm.onEvent(PracticeRunnerEvent.ConfirmSubmit)
            runCurrent()
            assertEquals(1, repository.submitCalls)
            assertEquals(3, repository.answers.size)
            assertEquals(PracticeRunnerEffect.OpenResult, vm.effects.first())
            vm.viewModelScope.cancel()
        }

    @Test
    fun aLinearLatihanAsksBeforeSkippingAnUnansweredQuestion() =
        runTest {
            val vm = viewModel(attempt(navigation = "linear"))

            vm.onEvent(PracticeRunnerEvent.Advance)
            assertEquals(RunnerDialog.Skip, vm.uiState.value.dialog)

            vm.onEvent(PracticeRunnerEvent.ConfirmSkip)
            runCurrent()
            assertEquals(RunnerDialog.None, vm.uiState.value.dialog)
            assertEquals(2L, vm.uiState.value.selectedItemId)
            assertEquals(listOf(1L, 2L), repository.activated)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aLatihanAlreadyFinishedOpensItsResult() =
        runTest {
            val vm = viewModel(attempt(status = "auto_submitted"))

            assertEquals(PracticeRunnerEffect.OpenResult, vm.effects.first())
            vm.viewModelScope.cancel()
        }

    @Test
    fun aLatihanCancelledByTheTeamStopsWithTheServerSentenceAndOnlyLeaves() =
        runTest {
            val vm = viewModel()
            repository.answerReply = Result.failure(ApiError(410, CANCELLED, "cancelled"))

            vm.onEvent(PracticeRunnerEvent.SelectOption(1))
            runCurrent()
            assertEquals(CANCELLED, vm.uiState.value.stopped)
            assertNull(vm.uiState.value.message)
            assertFalse(vm.uiState.value.isSaving)

            // Nothing more is read or saved, and leaving needs no save.
            val reads = repository.attemptCalls
            vm.onResume()
            advanceTimeBy(60_000)
            runCurrent()
            vm.onEvent(PracticeRunnerEvent.SelectOption(2))
            runCurrent()
            assertEquals(reads, repository.attemptCalls)
            assertEquals(1, repository.answers.size)

            vm.onEvent(PracticeRunnerEvent.Leave)
            assertEquals(PracticeRunnerEffect.Leave, vm.effects.first())
            vm.viewModelScope.cancel()
        }

    @Test
    fun aStudentNoLongerInTheMitraSeesWhyOnReadingAndOnSubmit() =
        runTest {
            val early = viewModelReading(Result.failure(ApiError(403, NOT_MEMBER, "not_member")))
            assertEquals(NOT_MEMBER, early.uiState.value.stopped)
            early.viewModelScope.cancel()

            val later = viewModel()
            repository.submitReply = Result.failure(ApiError(403, NOT_MEMBER, "not_member"))
            later.onEvent(PracticeRunnerEvent.ConfirmSubmit)
            runCurrent()
            assertEquals(NOT_MEMBER, later.uiState.value.stopped)
            assertFalse(later.uiState.value.isSubmitting)
            later.viewModelScope.cancel()
        }

    @Test
    fun aRefusalOtherThan422StillSaysTheServerSentenceFor403And410() {
        val message =
            PracticeRunnerViewModel.requestMessage(
                ApiError(410, CANCELLED, "cancelled"),
                RunnerMessage.LoadFailed,
            )
        assertEquals(RunnerMessage.Server(CANCELLED), message)
        assertEquals(
            RunnerMessage.LoadFailed,
            PracticeRunnerViewModel.requestMessage(
                ApiError(500, "Terjadi kesalahan.", "server"),
                RunnerMessage.LoadFailed,
            ),
        )
    }

    @Test
    fun comingBackToTheScreenReadsTheAttemptAtOnce() =
        runTest {
            val vm = viewModel()
            val reads = repository.attemptCalls

            // First shown: the read on opening is enough.
            vm.onResume()
            runCurrent()
            assertEquals(reads, repository.attemptCalls)

            vm.onPause()
            vm.onResume()
            runCurrent()
            assertEquals(reads + 1, repository.attemptCalls)
            vm.viewModelScope.cancel()
        }

    @Test
    fun whenTimeIsUpTheResultOpensOnceAndTheReadsStop() =
        runTest {
            val timed =
                attempt().copy(
                    timerMode = PracticeAttempt.TIMER_PACKAGE,
                    timerEnabled = true,
                    deadlineAt = "2026-11-20T03:00:30Z",
                )
            val vm = viewModel(timed)
            val effects = mutableListOf<PracticeRunnerEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects += it } }
            repository.attemptReply = Result.success(attempt(status = "auto_submitted"))

            advanceTimeBy(40_000)
            runCurrent()
            val reads = repository.attemptCalls
            advanceTimeBy(10_000)
            runCurrent()

            assertEquals(listOf(PracticeRunnerEffect.OpenResult), effects)
            assertEquals(reads, repository.attemptCalls)
            vm.viewModelScope.cancel()
        }

    private companion object {
        const val ATTEMPT = 1201L
        const val CANCELLED = "Pengerjaan ini dibatalkan oleh tim Gotest. Kamu bisa memulai paket ini lagi."
        const val NOT_MEMBER = "Kamu bukan lagi siswa Mitra ini, sehingga Latihan tidak bisa dilanjutkan."
    }
}
