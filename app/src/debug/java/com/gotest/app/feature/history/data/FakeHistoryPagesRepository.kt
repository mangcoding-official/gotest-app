package com.gotest.app.feature.history.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.history.domain.HistoryCategory
import com.gotest.app.feature.history.domain.HistoryDetail
import com.gotest.app.feature.history.domain.HistoryFilters
import com.gotest.app.feature.history.domain.HistoryMode
import com.gotest.app.feature.history.domain.HistoryPage
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryPagination
import com.gotest.app.feature.history.domain.HistoryRankingRow
import com.gotest.app.feature.history.domain.HistoryResult
import com.gotest.app.feature.history.domain.HistorySection
import com.gotest.app.feature.history.domain.HistorySort
import com.gotest.app.feature.history.domain.HistoryStats
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug Riwayat (debug builds only): 27 finished attempts over three months, so "Tampilkan lebih
 * banyak" shows (20 a page). Filters, paging, accuracy and the CSV follow StudentHistoryData.
 */
@Singleton
class FakeHistoryPagesRepository
    @Inject
    constructor() : HistoryPagesRepository {
        private val all: List<HistoryResult> = (1..TOTAL).map(::attempt)

        override suspend fun history(
            filters: HistoryFilters,
            page: Int,
        ): Result<HistoryPage> {
            simulateLatency()
            val matching =
                all
                    .filter { filters.q.isBlank() || it.packageName.contains(filters.q.trim(), ignoreCase = true) }
                    .filter { filters.mode == HistoryMode.All || it.mode == filters.mode.query }
                    .filter {
                        filters.category == null ||
                            it.categoryName == CATEGORIES.first { c -> c.id == filters.category }.name
                    }.let { if (filters.sort == HistorySort.Oldest) it.reversed() else it }
            val from = (page - 1) * PER_PAGE
            return Result.success(
                HistoryPage(
                    categories = CATEGORIES,
                    filters = filters,
                    attempts = matching.drop(from).take(PER_PAGE),
                    pagination =
                        HistoryPagination(
                            nextPage = (page + 1).takeIf { matching.size > from + PER_PAGE },
                            total = matching.size,
                        ),
                    stats = HistoryStats(questionsDone = 480, accuracy = 68, packagesDone = 19),
                ),
            )
        }

        override suspend fun detail(attemptId: Long): Result<HistoryDetail> {
            simulateLatency()
            val found =
                all.firstOrNull { it.id == attemptId }
                    ?: return Result.failure(ApiError(NOT_FOUND, "Data tidak ditemukan.", "not_found"))
            val sections =
                if (found.isTryout) {
                    listOf(
                        HistorySection("Penalaran Umum", 18, 9, 3),
                        HistorySection("Pengetahuan Kuantitatif", 11, 6, 3),
                        HistorySection("Literasi Bahasa Indonesia", 21, 7, 2),
                    )
                } else {
                    listOf(HistorySection("Matematika", 14, 4, 2))
                }
            val ranking =
                if (found.isTryout && found.sessionTitle == null) {
                    (1..5).map {
                        HistoryRankingRow(
                            it,
                            NAMES[it],
                            790.0 - it * 12.5,
                            "2026-11-1${it}T03:00:00+00:00",
                            "1${it}0 Menit ${it * 7} Detik",
                        )
                    }
                } else {
                    emptyList()
                }
            return Result.success(HistoryDetail(found.copy(packageId = 11, sections = sections), ranking))
        }

        override suspend fun export(): Result<InputStream> {
            simulateLatency()
            val wib = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Jakarta"))
            val csv =
                buildString {
                    appendLine("Paket,Jenjang,Tipe,Tanggal,Durasi,Akurasi,Skor")
                    all.forEach {
                        val type = if (it.isTryout) "Try Out" else "Latihan"
                        val date = it.submittedAt?.let { s -> wib.format(Instant.parse(s)) } ?: "-"
                        appendLine(
                            "${it.packageName},${it.categoryName},$type,$date,${it.durationLabel},${it.accuracy}%,${it.score}",
                        )
                    }
                }
            return Result.success(ByteArrayInputStream(csv.toByteArray()))
        }

        private fun attempt(index: Int): HistoryResult {
            val tryout = index % 3 == 0
            val submitted = START.minus(index * 3L, ChronoUnit.DAYS)
            val seconds = 1_200 + index * 97
            return HistoryResult(
                id = 100L + index,
                packageName = if (tryout) "Try Out UTBK-SNBT ${index / 3}" else PRACTICE[index % PRACTICE.size],
                sessionTitle = "Try Out Bersama November".takeIf { tryout && index % 9 == 0 },
                description = "Latihan soal terpilih dari bank soal Gotest.".takeIf { !tryout },
                categoryName = if (index % 2 == 0) "UTBK-SNBT" else "TKA SMA",
                mode = if (tryout) "tryout" else "practice",
                submittedAt = submitted.toString(),
                durationSeconds = seconds,
                durationLabel = "${seconds / 60} Menit ${seconds % 60} Detik",
                accuracy = 50 + index % 45,
                score = if (tryout) 480.5 + index * 3 else (60 + index % 40).toDouble(),
                scoreLabel = (if (index % 4 == 1) "Skor Latihan" else "Nilai").takeIf { !tryout },
                ranked = (index % 6 != 0).takeIf { tryout },
                disqualified = (index == 12).takeIf { tryout },
            )
        }

        private companion object {
            const val TOTAL = 27
            const val PER_PAGE = 20
            const val NOT_FOUND = 404
            val START: Instant = Instant.parse("2026-11-17T08:30:00Z")
            val CATEGORIES = listOf(HistoryCategory(1, "TKA SMA"), HistoryCategory(2, "UTBK-SNBT"))
            val PRACTICE = listOf("Latihan Aljabar", "Latihan Penalaran Umum", "Latihan Literasi", "Latihan Geometri")
            val NAMES = listOf("", "Rina Kartika", "Bagus Pratama", "Salsa Nabila", "Gilang Permana", "Dimas Aryo")
        }
    }
