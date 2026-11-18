package com.gotest.app.feature.history

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.gotest.app.feature.history.data.HistoryCsvStore
import com.gotest.app.feature.history.domain.HistoryDetail
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryPage
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryPagination
import com.gotest.app.feature.history.ui.history.HistoryViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.io.InputStream

/** A2e review 3: an answer for an older filter never lands on the newer list. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    /** Each search answers when the test says so. */
    private class WaitingRepository : HistoryPagesRepository {
        val replies = mutableMapOf<String, CompletableDeferred<Result<HistoryPage>>>()

        fun reply(q: String) = replies.getOrPut(q) { CompletableDeferred() }

        override suspend fun history(
            filters: HistoryFilters,
            page: Int,
        ): Result<HistoryPage> = reply(filters.q).await()

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
    ) = Result.success(HistoryPage(filters = HistoryFilters(q = q), pagination = HistoryPagination(total = total)))

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
}
