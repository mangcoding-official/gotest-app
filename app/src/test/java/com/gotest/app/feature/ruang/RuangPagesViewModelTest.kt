package com.gotest.app.feature.ruang

import androidx.lifecycle.SavedStateHandle
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.ScriptedPracticeRepository
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.ui.catalog.StartProblem
import com.gotest.app.feature.ruang.domain.InvitationPage
import com.gotest.app.feature.ruang.domain.JoinCodePage
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.domain.MitraMembership
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.RoomPlan
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangPractice
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.StudyPlanPage
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import com.gotest.app.feature.ruang.domain.TaskRecord
import com.gotest.app.feature.ruang.domain.TaskStart
import com.gotest.app.feature.ruang.ui.plan.PlanViewModel
import com.gotest.app.feature.ruang.ui.space.PracticeAction
import com.gotest.app.feature.ruang.ui.space.RuangEffect
import com.gotest.app.feature.ruang.ui.space.RuangViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The Mitra space and Rencana pages (A2c): a reload cancels the request still on its way and asks again (Ardy's
 * review of Rencana Belajar), "Pilih rencana" survives, and the Latihan sekolah buttons.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RuangPagesViewModelTest {
    /** Each page request answers when the test says so, in the order asked. */
    private class WaitingRuang : RuangRepository {
        val replies = mutableListOf<CompletableDeferred<Result<RuangPage>>>()

        override suspend fun memberships(): Result<List<MitraMembership>> =
            Result.success(listOf(MitraMembership(CODE, "SMP 1 Bantargedung", "sekolah", "siswa", hasExams = true)))

        override suspend fun signInEndsAt(): Result<String?> = Result.success(null)

        override suspend fun ruang(code: String): Result<RuangPage> =
            CompletableDeferred<Result<RuangPage>>().also { replies += it }.await()

        override suspend fun lookupCode(typed: String): Result<String> = Result.failure(IOException())

        override suspend fun joinCode(code: String): Result<JoinCodePage> = Result.failure(IOException())

        override suspend fun join(code: String): Result<JoinedMitra> = Result.failure(IOException())

        override suspend fun invitation(token: String): Result<InvitationPage> = Result.failure(IOException())

        override suspend fun sendInvitationCode(token: String): Result<String?> = Result.failure(IOException())

        override suspend fun acceptInvitation(
            token: String,
            code: String?,
        ): Result<JoinedMitra> = Result.failure(IOException())
    }

    private class WaitingPlans : StudyPlanRepository {
        val replies = mutableListOf<CompletableDeferred<Result<StudyPlanPage>>>()

        override suspend fun plan(code: String): Result<StudyPlanPage> =
            CompletableDeferred<Result<StudyPlanPage>>().also { replies += it }.await()

        override suspend fun start(
            code: String,
            taskId: Long,
        ): Result<TaskStart> = Result.failure(IOException())

        override suspend fun record(
            code: String,
            taskId: Long,
            record: TaskRecord,
        ): Result<String?> = Result.failure(IOException())

        override suspend fun unrecord(
            code: String,
            taskId: Long,
        ): Result<String?> = Result.failure(IOException())
    }

    private val ruang = WaitingRuang()
    private val plans = WaitingPlans()
    private val practice = ScriptedPracticeRepository()

    private fun TestScope.ruangViewModel(): RuangViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return RuangViewModel(
            SavedStateHandle(mapOf(RuangViewModel.ARG_CODE to CODE)),
            ruang,
            practice,
            plans,
            testLocalStore(),
        )
    }

    private fun TestScope.planViewModel(
        state: SavedStateHandle = SavedStateHandle(mapOf("code" to CODE)),
    ): PlanViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return PlanViewModel(state, plans, ruang, testLocalStore())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun page(vararg practice: RuangPractice) =
        Result.success(RuangPage(MitraRef("SMP 1 Bantargedung", CODE), practice = practice.toList()))

    private fun plan(
        id: Long,
        title: String,
    ) = RoomPlan(id, title, startsOn = "2026-11-09", examDate = "2026-12-06", state = "running", weekCount = 4)

    private fun planPage(vararg plans: RoomPlan) =
        Result.success(StudyPlanPage(MitraRef("SMP 1 Bantargedung", CODE), plans = plans.toList()))

    @Test
    fun ruangReloadCancelsTheRequestOnItsWayAndShowsTheNewest() =
        runTest {
            val vm = ruangViewModel()
            runCurrent()
            ruang.replies[0].complete(page(SCHOOL.copy(name = "Pertama")))
            runCurrent()

            vm.refresh()
            runCurrent()
            vm.refresh()
            runCurrent()
            assertEquals(3, ruang.replies.size)

            // The newest answers first; the cancelled one answering later changes nothing.
            ruang.replies[2].complete(page(SCHOOL.copy(name = "Terbaru")))
            runCurrent()
            ruang.replies[1].complete(page(SCHOOL.copy(name = "Lama")))
            runCurrent()

            assertEquals(
                "Terbaru",
                vm.uiState.value.page
                    ?.practice
                    ?.single()
                    ?.name,
            )
            assertTrue(vm.uiState.value.hasExams)
        }

    @Test
    fun ruangReloadBeforeThePageArrivedWaitsForTheFirstRequest() =
        runTest {
            val vm = ruangViewModel()
            runCurrent()
            vm.refresh()
            runCurrent()

            assertEquals(1, ruang.replies.size)
        }

    @Test
    fun startingALatihanSekolahOpensItsAttempt() =
        runTest {
            val vm = ruangViewModel()
            runCurrent()
            ruang.replies[0].complete(page(SCHOOL))
            runCurrent()
            practice.startReply = Result.success(PracticeAttempt(1301, "started", "2026-11-20T03:00:00Z"))

            vm.onPractice(SCHOOL, PracticeAction.Start)
            assertEquals(SCHOOL.id, vm.uiState.value.startingPracticeId)
            runCurrent()

            assertEquals(listOf(CODE to SCHOOL.id), practice.ruangStarts)
            assertEquals(RuangEffect.OpenPractice(1301, SCHOOL.name), vm.effects.first())
            assertNull(vm.uiState.value.startingPracticeId)
        }

    @Test
    fun aRefusedStartShowsWhyUnderItsCard() =
        runTest {
            val vm = ruangViewModel()
            runCurrent()
            ruang.replies[0].complete(page(SCHOOL))
            runCurrent()

            practice.startReply = Result.failure(ApiError(422, "Latihan ini sudah ditutup.", "validation"))
            vm.onPractice(SCHOOL, PracticeAction.Start)
            runCurrent()
            assertEquals(SCHOOL.id to StartProblem.Server("Latihan ini sudah ditutup."), vm.uiState.value.practiceError)

            practice.startReply = ScriptedPracticeRepository.offline()
            vm.onPractice(SCHOOL, PracticeAction.Start)
            runCurrent()
            assertEquals(SCHOOL.id to StartProblem.Offline, vm.uiState.value.practiceError)

            practice.startReply = Result.failure(ApiError(404, "Data tidak ditemukan.", "not_found"))
            vm.onPractice(SCHOOL, PracticeAction.Start)
            runCurrent()
            assertEquals(SCHOOL.id to StartProblem.Fallback, vm.uiState.value.practiceError)
        }

    @Test
    fun continueAndReviewOpenTheirAttemptWithoutARequest() =
        runTest {
            val vm = ruangViewModel()
            val item = SCHOOL.copy(continueAttemptId = 1290, lastAttemptId = 903)

            vm.onPractice(item, PracticeAction.Continue)
            assertEquals(RuangEffect.OpenPractice(1290, item.name), vm.effects.first())
            vm.onPractice(item, PracticeAction.Review)
            assertEquals(RuangEffect.OpenPracticeResult(903, item.name), vm.effects.first())
            assertTrue(practice.ruangStarts.isEmpty())
        }

    @Test
    fun planReloadCancelsTheRequestOnItsWayAndShowsTheNewest() =
        runTest {
            val vm = planViewModel()
            runCurrent()
            plans.replies[0].complete(planPage(plan(31, "Pertama")))
            runCurrent()

            vm.refresh()
            runCurrent()
            vm.refresh()
            runCurrent()
            assertEquals(3, plans.replies.size)

            plans.replies[2].complete(planPage(plan(31, "Terbaru")))
            runCurrent()
            plans.replies[1].complete(planPage(plan(31, "Lama")))
            runCurrent()

            assertEquals(
                "Terbaru",
                vm.uiState.value.plan
                    ?.title,
            )
        }

    @Test
    fun aChosenPlanStaysAfterTheScreenIsRecreated() =
        runTest {
            val state = SavedStateHandle(mapOf("code" to CODE))
            val first = planViewModel(state)
            runCurrent()
            plans.replies[0].complete(planPage(plan(31, "Utama"), plan(32, "Ujian Akhir")))
            runCurrent()
            assertEquals(
                31L,
                first.uiState.value.plan
                    ?.id,
            )

            first.choose(32)
            assertEquals(
                32L,
                first.uiState.value.plan
                    ?.id,
            )

            val again = planViewModel(state)
            runCurrent()
            plans.replies[1].complete(planPage(plan(31, "Utama"), plan(32, "Ujian Akhir")))
            runCurrent()
            assertEquals(
                32L,
                again.uiState.value.plan
                    ?.id,
            )
        }

    @Test
    fun aChosenPlanThatIsGoneFallsBackToTheMainPlan() =
        runTest {
            val vm = planViewModel(SavedStateHandle(mapOf("code" to CODE, "plan.chosen" to 99L)))
            runCurrent()
            plans.replies[0].complete(planPage(plan(31, "Utama")))
            runCurrent()

            assertEquals(
                31L,
                vm.uiState.value.plan
                    ?.id,
            )
        }

    private companion object {
        const val CODE = "SMP1BTG"
        val SCHOOL = RuangPractice(601, "Latihan Pecahan Kelas 8", questionCount = 5, canStart = true)
    }
}
