package com.gotest.app.feature.history

import com.gotest.app.core.designsystem.component.GotestChipTone
import com.gotest.app.feature.history.domain.HistoryResult
import com.gotest.app.feature.history.ui.history.categoryTone
import com.gotest.app.feature.history.ui.history.groupByMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.util.TimeZone

class HistoryGroupingTest {
    private fun result(
        id: Long,
        at: String?,
    ) = HistoryResult(
        id = id,
        packageName = "P$id",
        categoryName = "TKA SMA",
        mode = "practice",
        submittedAt = at,
        durationLabel = "",
    )

    @Test
    fun `results group by the phone's month in list order, undated ones together`() {
        val zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jakarta"))
        try {
            val groups =
                groupByMonth(
                    listOf(
                        result(1, "2026-11-02T10:00:00+00:00"),
                        // 31 Oktober 20.00 UTC is already 1 November in WIB.
                        result(2, "2026-10-31T20:00:00+00:00"),
                        result(3, null),
                        result(4, "2026-10-30T08:00:00+00:00"),
                        result(5, "bukan tanggal"),
                    ),
                )

            assertEquals(listOf(YearMonth.of(2026, 11), null, YearMonth.of(2026, 10)), groups.map { it.first })
            assertEquals(listOf(1L, 2L), groups[0].second.map(HistoryResult::id))
            assertEquals(listOf(3L, 5L), groups[1].second.map(HistoryResult::id))
        } finally {
            TimeZone.setDefault(zone)
        }
    }

    @Test
    fun `level badges follow the web colours`() {
        assertEquals(GotestChipTone.Green, categoryTone("TKA SMA"))
        assertEquals(GotestChipTone.Blue, categoryTone("UTBK-SNBT"))
        assertEquals(GotestChipTone.Violet, categoryTone("CPNS"))
    }
}
