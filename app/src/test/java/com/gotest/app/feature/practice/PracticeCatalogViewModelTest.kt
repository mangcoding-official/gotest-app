package com.gotest.app.feature.practice

import androidx.lifecycle.SavedStateHandle
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.practice.domain.CatalogCategory
import com.gotest.app.feature.practice.domain.CatalogSubject
import com.gotest.app.feature.practice.domain.PracticeAttempt
import com.gotest.app.feature.practice.domain.PracticeCatalogPage
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.practice.ui.catalog.DateFilter
import com.gotest.app.feature.practice.ui.catalog.PracticeCatalogEffect
import com.gotest.app.feature.practice.ui.catalog.PracticeCatalogViewModel
import com.gotest.app.feature.practice.ui.catalog.StartProblem
import com.gotest.app.feature.practice.ui.catalog.groupBySubject
import com.gotest.app.feature.practice.ui.catalog.matchesDate
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Paket Latihan (A2c): the start sheet's three ways (web PracticeStartModal), grouping, and the date filter. */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeCatalogViewModelTest {
    private val repository = ScriptedPracticeRepository()

    private fun TestScope.viewModel(vararg packages: PracticePackage): PracticeCatalogViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        repository.catalogReply =
            Result.success(PracticeCatalogPage(listOf(CatalogCategory(1, "UTBK-SNBT")), packages.toList()))
        return PracticeCatalogViewModel(SavedStateHandle(), repository).also { runCurrent() }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aRunningLatihanContinuesWithoutANewStart() =
        runTest {
            val item = PracticePackage(101, "Penalaran Umum 1", continueAttemptId = 1201)
            val vm = viewModel(item)

            vm.openSheet(item)
            vm.start()

            assertEquals(PracticeCatalogEffect.OpenRunner(1201, item.name), vm.effects.first())
            assertTrue(repository.starts.isEmpty())
            assertNull(vm.uiState.value.sheet)
        }

    @Test
    fun aFinishedLatihanWithoutRetryOpensItsPembahasan() =
        runTest {
            val item = PracticePackage(104, "Literasi 1", allowRetry = false, finishedCount = 1, lastAttemptId = 901)
            val vm = viewModel(item)

            vm.openSheet(item)
            vm.start()

            assertEquals(PracticeCatalogEffect.OpenResult(901, item.name), vm.effects.first())
            assertTrue(repository.starts.isEmpty())
        }

    @Test
    fun theTimerChoiceIsSentOnlyWhenTheTimerIsOptional() =
        runTest {
            val optional = PracticePackage(102, "Kuantitatif 1", timerOptional = true)
            val fixed = PracticePackage(103, "Kuantitatif 2")
            val vm = viewModel(optional, fixed)
            repository.startReply = Result.success(PracticeAttempt(1202, "started", "2026-11-20T03:00:00Z"))

            vm.openSheet(optional)
            vm.onUseTimer(false)
            vm.start()
            runCurrent()
            assertEquals(PracticeCatalogEffect.OpenRunner(1202, optional.name), vm.effects.first())

            vm.openSheet(fixed)
            vm.start()
            runCurrent()

            assertEquals(listOf(102L to false, 103L to null), repository.starts)
        }

    @Test
    fun aRefusedStartStaysOnTheSheetWithTheServerSentence() =
        runTest {
            val item = PracticePackage(105, "Penalaran Umum 2")
            val vm = viewModel(item)
            repository.startReply =
                Result.failure(
                    ApiError(
                        422,
                        "Data tidak valid.",
                        "validation",
                        mapOf("package" to listOf("Paket ini tidak dapat dikerjakan ulang.")),
                    ),
                )

            vm.openSheet(item)
            vm.start()
            runCurrent()

            val sheet = vm.uiState.value.sheet
            assertFalse(sheet?.isStarting ?: true)
            assertEquals(StartProblem.Server("Paket ini tidak dapat dikerjakan ulang."), sheet?.error)

            // Gratis quota, Premium (403), or busy (429): the server's sentence too (A2c review, item 3).
            repository.startReply = Result.failure(ApiError(403, "Kuota habis.", "quota_used"))
            vm.start()
            runCurrent()
            assertEquals(
                StartProblem.Server("Kuota habis."),
                vm.uiState.value.sheet
                    ?.error,
            )

            repository.startReply =
                Result.failure(ApiError(429, "Permintaan sebelumnya masih diproses. Coba lagi sebentar.", "busy"))
            vm.start()
            runCurrent()
            assertEquals(
                StartProblem.Server("Permintaan sebelumnya masih diproses. Coba lagi sebentar."),
                vm.uiState.value.sheet
                    ?.error,
            )

            repository.startReply = Result.failure(ApiError(500, "Terjadi kesalahan.", "server"))
            vm.start()
            runCurrent()
            assertEquals(
                StartProblem.Fallback,
                vm.uiState.value.sheet
                    ?.error,
            )
        }

    @Test
    fun theSheetCannotCloseWhileStarting() =
        runTest {
            val item = PracticePackage(105, "Penalaran Umum 2")
            val vm = viewModel(item)

            vm.openSheet(item)
            vm.start()
            vm.closeSheet()
            assertTrue(
                vm.uiState.value.sheet
                    ?.isStarting == true,
            )

            runCurrent()
            vm.closeSheet()
            assertNull(vm.uiState.value.sheet)
        }

    @Test
    fun packagesGroupUnderEachSubjectWithUmumLast() {
        val reasoning = CatalogSubject(11, "Penalaran Umum")
        val math = CatalogSubject(12, "Matematika")
        val groups =
            groupBySubject(
                listOf(
                    PracticePackage(1, "A", subjects = listOf(reasoning, math), topicIds = listOf(1, 2)),
                    PracticePackage(2, "B", subjects = listOf(reasoning), topicIds = listOf(2, 3), finishedCount = 1),
                    PracticePackage(3, "C"),
                ),
            )

        assertEquals(listOf("Matematika", "Penalaran Umum", ""), groups.map { it.subject.name })
        val reasoningGroup = groups[1]
        assertEquals(listOf(1L, 2L), reasoningGroup.packages.map { it.id })
        assertEquals(3, reasoningGroup.topicCount)
        assertEquals(1, reasoningGroup.finishedPackages)
    }

    @Test
    fun theDateFilterCountsFromNowAndTheYearInWib() {
        val now = Instant.parse("2026-11-20T03:00:00Z")

        assertTrue(matchesDate("2026-11-14T03:00:00+00:00", DateFilter.Days7, now))
        assertFalse(matchesDate("2026-11-12T03:00:00+00:00", DateFilter.Days7, now))
        assertTrue(matchesDate("2026-10-22T03:00:00+00:00", DateFilter.Days30, now))
        // 31 December 18:00 UTC is already 1 January in WIB.
        assertTrue(matchesDate("2025-12-31T18:00:00+00:00", DateFilter.Year, now))
        assertFalse(matchesDate(null, DateFilter.Days30, now))
        assertTrue(matchesDate(null, DateFilter.All, now))
    }
}
