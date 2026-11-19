package com.gotest.app.feature.history

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.history.data.HistoryCsvStore
import com.gotest.app.feature.history.domain.HistoryDetail
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryPage
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryPagination
import com.gotest.app.feature.history.ui.history.HistoryEffect
import com.gotest.app.feature.history.ui.history.HistoryViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException
import java.io.InputStream

/** A2e review 3 and review ulang 1: the list and its filters change only when a new list arrives. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    /** Each search answers when the test says so. */
    private class WaitingRepository : HistoryPagesRepository {
        val replies = mutableMapOf<String, CompletableDeferred<Result<HistoryPage>>>()
        val asked = mutableListOf<Pair<String, Int>>()

        fun reply(
            q: String,
            page: Int = 1,
        ) = replies.getOrPut(if (page == 1) q else "$q#$page") { CompletableDeferred() }

        override suspend fun history(
            filters: HistoryFilters,
            page: Int,
        ): Result<HistoryPage> {
            asked += filters.q to page
            return reply(filters.q, page).await()
        }

        override suspend fun detail(attemptId: Long): Result<HistoryDetail> = Result.failure(IOException())

        override suspend fun export(): Result<InputStream> = Result.failure(IOException())
    }

    private val repository = WaitingRepository()
    private val csv =
        object : HistoryCsvStore {
            override suspend fun save(csv: InputStream): Result<Uri> = Result.failure(IOException())
        }

    private fun page(
        q: String,
        total: Int,
        nextPage: Int? = null,
    ) = Result.success(
        HistoryPage(
            filters = HistoryFilters(q = q),
            pagination = HistoryPagination(total = total, nextPage = nextPage),
        ),
    )

    private fun TestScope.viewModel(): HistoryViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return HistoryViewModel(repository, csv)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun anOlderFilterAnsweringLateIsDropped() =
        runTest {
            val vm = viewModel()
            runCurrent()
            repository.reply("").complete(page("", total = 40))
            runCurrent()

            vm.onDraftChange(HistoryFilters(q = "a"))
            vm.applyFilters()
            runCurrent()
            vm.onDraftChange(HistoryFilters(q = "ab"))
            vm.applyFilters()
            runCurrent()
            // "ab" answers first, then the slower "a".
            repository.reply("ab").complete(page("ab", total = 2))
            runCurrent()
            repository.reply("a").complete(page("a", total = 9))
            runCurrent()

            assertEquals("ab", vm.uiState.value.applied.q)
            assertEquals(2, vm.uiState.value.total)
            vm.viewModelScope.cancel()
        }

    @Test
    fun aFailedFilterKeepsTheListAndItsFilters() =
        runTest {
            val vm = viewModel()
            val effects = mutableListOf<HistoryEffect>()
            backgroundScope.launch { vm.effects.collect { effects += it } }
            runCurrent()
            repository.reply("").complete(page("", total = 40, nextPage = 2))
            runCurrent()

            vm.onDraftChange(HistoryFilters(q = "a"))
            vm.applyFilters()
            runCurrent()
            repository.reply("a").complete(Result.failure(IOException()))
            runCurrent()

            assertEquals("", vm.uiState.value.applied.q)
            assertEquals("a", vm.uiState.value.draft.q)
            assertFalse(vm.uiState.value.loadFailed)
            assertEquals(listOf<HistoryEffect>(HistoryEffect.ListFailed), effects)
            // "Tampilkan lebih banyak" goes on with the list on screen, not the filter that failed.
            vm.loadMore()
            runCurrent()
            assertEquals("" to 2, repository.asked.last())
            vm.viewModelScope.cancel()
        }
}
