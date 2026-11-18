package com.gotest.app.feature.ruang

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.ruang.data.ElapsedClock
import com.gotest.app.feature.ruang.domain.ExamAnswer
import com.gotest.app.feature.ruang.domain.ExamAttemptInfo
import com.gotest.app.feature.ruang.domain.ExamBlock
import com.gotest.app.feature.ruang.domain.ExamItem
import com.gotest.app.feature.ruang.domain.ExamItemType
import com.gotest.app.feature.ruang.domain.ExamViolationResult
import com.gotest.app.feature.ruang.domain.ExamWorkPage
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.ui.work.ExamWorkEffect
import com.gotest.app.feature.ruang.ui.work.ExamWorkEvent
import com.gotest.app.feature.ruang.ui.work.ExamWorkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExamWorkViewModelTest {
    private val repository = ScriptedExamRepository()
    private val local = testLocalStore()
    private val serverNow = Instant.parse("2026-12-01T01:30:00Z")

    private fun page(secondsLeft: Long) =
        ExamWorkPage(
            organization = MitraRef("SMP 1 Bantargedung", "SMP1BTG"),
            title = "PTS Matematika Kelas 8",
            attempt =
                ExamAttemptInfo(
                    id = 9,
                    status = "in_progress",
                    deadlineAt = serverNow.plusSeconds(secondsLeft).toString(),
                    serverNow = serverNow.toString(),
                    violations = 0,
                    violationLimit = 3,
                    deviceToken = "token",
                ),
            blocks = listOf(ExamBlock("single", listOf(55))),
            items =
                listOf(
                    ExamItem(
                        id = 55,
                        number = 1,
                        type = ExamItemType.SINGLE,
                        prompt = "<p>Soal</p>",
                        content = buildJsonObject { put("options", kotlinx.serialization.json.JsonArray(emptyList())) },
                    ),
                ),
        )

    private fun TestScope.viewModel(): ExamWorkViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        local.saveDeviceToken(9, "token")
        return ExamWorkViewModel(
            SavedStateHandle(mapOf("code" to "SMP1BTG", "attemptId" to 9L)),
            repository,
            local,
            ElapsedClock { testScheduler.currentTime },
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun answersAreRefusedUntilTheScreenIsPinned() =
        runTest {
            repository.workPage = Result.success(page(secondsLeft = 3_600))
            val vm = viewModel()
            runCurrent()
            assertTrue(vm.uiState.value.awaitingPin)

            vm.onEvent(ExamWorkEvent.Answer(55, ExamAnswer.Single("b")))
            assertTrue(
                vm.uiState.value.answers
                    .isEmpty(),
            )
            assertTrue(local.pending(9).isEmpty())

            vm.onPinnedChange(true)
            assertFalse(vm.uiState.value.awaitingPin)
            vm.onEvent(ExamWorkEvent.Answer(55, ExamAnswer.Single("b")))
            assertEquals(ExamAnswer.Single("b"), vm.uiState.value.answers[55])
            assertEquals(setOf(55L), local.pending(9).keys)
            vm.viewModelScope.cancel()
        }

    @Test
    fun timeUpOpensTheResultOnce() =
        runTest {
            repository.workPage = Result.success(page(secondsLeft = 2))
            val vm = viewModel()
            val effects = mutableListOf<ExamWorkEffect>()
            backgroundScope.launch { vm.effects.collect { effects += it } }
            vm.onPinnedChange(true)
            advanceTimeBy(15_000)
            runCurrent()
            assertTrue(vm.uiState.value.timeUp)
            assertEquals(listOf(ExamWorkEffect.OpenResult), effects)
            vm.viewModelScope.cancel()
        }

    @Test
    fun timeAwayWhileTheAppWasClosedIsReportedOnReopening() =
        runTest {
            repository.workPage = Result.success(page(secondsLeft = 3_600))
            repository.violationReplies += Result.success(ExamViolationResult(1, 3, "in_progress"))
            local.saveAwaySince(9, serverNow.minusSeconds(30).toString())
            val vm = viewModel()
            runCurrent()
            assertEquals(30_000L, repository.reportedViolations.single().awayMs)
            assertEquals(1, vm.uiState.value.violationWarning)
            vm.viewModelScope.cancel()
        }

    @Test
    fun leavingThroughKeluarIsNotAViolation() =
        runTest {
            repository.workPage = Result.success(page(secondsLeft = 3_600))
            val vm = viewModel()
            runCurrent()
            vm.onPinnedChange(true)
            vm.onEvent(ExamWorkEvent.ConfirmExit)
            // The screen lets go of the pinned screen on its way out.
            vm.onLeave()
            runCurrent()
            assertEquals(null, local.awaySince(9))
            vm.viewModelScope.cancel()
        }
}
