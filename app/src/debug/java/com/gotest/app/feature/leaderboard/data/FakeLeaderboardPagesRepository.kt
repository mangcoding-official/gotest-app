package com.gotest.app.feature.leaderboard.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.leaderboard.domain.LeaderboardEntry
import com.gotest.app.feature.leaderboard.domain.LeaderboardFilters
import com.gotest.app.feature.leaderboard.domain.LeaderboardOptions
import com.gotest.app.feature.leaderboard.domain.LeaderboardPackageCard
import com.gotest.app.feature.leaderboard.domain.LeaderboardPage
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.LeaderboardScope
import com.gotest.app.feature.leaderboard.domain.NamedOption
import com.gotest.app.feature.leaderboard.domain.PackageBoardEntry
import com.gotest.app.feature.leaderboard.domain.PackageBoardInfo
import com.gotest.app.feature.leaderboard.domain.PackageBoardPage
import com.gotest.app.feature.leaderboard.domain.TaxonomyBranch
import com.gotest.app.feature.leaderboard.domain.TaxonomyHierarchy
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug data for the Leaderboard (debug builds only): 137 participants on the global board with
 * Gilang at 42, two Try Out packages (one with its ranking not open yet), and two Latihan packages.
 * Shapes and rules follow LeaderboardPageData (points 0–1,000, limit 20–100, top 3 + rows).
 */
@Singleton
class FakeLeaderboardPagesRepository
    @Inject
    constructor() : LeaderboardPagesRepository {
        override suspend fun leaderboard(filters: LeaderboardFilters): Result<LeaderboardPage> {
            simulateLatency()
            val limit = filters.limit.coerceIn(LeaderboardFilters.DEFAULT_LIMIT, LeaderboardFilters.MAX_LIMIT)
            val everyone = ranking(seed = (filters.category ?: 0) + (filters.subject ?: 0) + filters.period.ordinal)
            val shown = everyone.take(limit)
            return Result.success(
                LeaderboardPage(
                    filters = filters.copy(limit = limit),
                    options = LeaderboardOptions(CATEGORIES, SUBJECTS),
                    top = shown.take(PODIUM),
                    rows = shown.drop(PODIUM),
                    current = everyone.firstOrNull { it.isCurrentUser },
                    packageCards =
                        when (filters.scope) {
                            LeaderboardScope.Global -> {
                                emptyList()
                            }

                            LeaderboardScope.Package -> {
                                TRYOUT_CARDS.filter {
                                    filters.category == null ||
                                        it.categoryName == "UTBK-SNBT"
                                }
                            }

                            LeaderboardScope.Practice -> {
                                PRACTICE_CARDS
                            }
                        },
                    taxonomyHierarchy = TAXONOMY,
                    totalParticipants = everyone.size,
                    hasMore = everyone.size > limit,
                    updatedAt = Instant.now().toString(),
                ),
            )
        }

        override suspend fun tryoutBoard(packageId: Long): Result<PackageBoardPage> {
            simulateLatency()
            val card = TRYOUT_CARDS.firstOrNull { it.id == packageId } ?: return notFound()
            val opensLater = packageId == WINDOWED_ID
            val rows =
                if (opensLater) {
                    emptyList()
                } else {
                    (1..card.totalParticipants).map { rank ->
                        PackageBoardEntry(
                            rank = rank,
                            userId = rank.toLong(),
                            name = if (rank == card.rank) ME else NAMES[rank % NAMES.size],
                            points = 780 - rank * 6,
                            durationUsed = 6_200 + rank * 41,
                            change = listOf(1, 0, -2, 3)[rank % 4],
                            isCurrentUser = rank == card.rank,
                        )
                    }
                }
            return Result.success(
                PackageBoardPage(
                    board =
                        PackageBoardInfo(
                            id = card.id,
                            name = card.name,
                            questionCount = card.questionCount,
                            durationMinutes = card.durationMinutes,
                            rankingOpensAt = "2026-11-20T03:15:00+00:00".takeIf { opensLater },
                        ),
                    top = rows.take(PODIUM),
                    rows = rows.drop(PODIUM),
                    current = rows.firstOrNull { it.isCurrentUser },
                    totalParticipants = rows.size,
                ),
            )
        }

        override suspend fun practiceBoard(packageId: Long): Result<PackageBoardPage> {
            simulateLatency()
            val card = PRACTICE_CARDS.firstOrNull { it.id == packageId } ?: return notFound()
            val rows =
                (1..card.totalParticipants).map { rank ->
                    PackageBoardEntry(
                        rank = rank,
                        userId = rank.toLong(),
                        name = if (rank == card.rank) ME else NAMES[(rank + 3) % NAMES.size],
                        points = 100 - rank * 3,
                        submittedAt = "2026-11-${(10 + rank % 8).toString().padStart(2, '0')}T0${rank % 9}:20:00+00:00",
                        change = 0,
                        isCurrentUser = rank == card.rank,
                    )
                }
            return Result.success(
                PackageBoardPage(
                    mode = "practice",
                    board =
                        PackageBoardInfo(
                            id = card.id,
                            name = card.name,
                            questionCount = card.questionCount,
                            timeLabel = card.timeLabel,
                            pointsLabel = card.pointsLabel,
                        ),
                    top = rows.take(PODIUM),
                    rows = rows.drop(PODIUM),
                    current = rows.firstOrNull { it.isCurrentUser },
                    totalParticipants = rows.size,
                ),
            )
        }

        private fun ranking(seed: Long): List<LeaderboardEntry> =
            (1..PARTICIPANTS).map { rank ->
                LeaderboardEntry(
                    rank = rank,
                    userId = rank.toLong(),
                    name = if (rank == MY_RANK) ME else NAMES[((rank + seed) % NAMES.size).toInt()],
                    attempts = 1 + rank % 6,
                    points = 1_000 - rank * 5,
                    change = listOf(2, 0, -1, 4, -3)[rank % 5],
                    isCurrentUser = rank == MY_RANK,
                )
            }

        private fun <T> notFound(): Result<T> =
            Result.failure(ApiError(NOT_FOUND, "Data tidak ditemukan.", "not_found"))

        private companion object {
            const val PARTICIPANTS = 137
            const val MY_RANK = 42
            const val PODIUM = 3
            const val ME = "Gilang Permana"
            const val WINDOWED_ID = 12L
            const val NOT_FOUND = 404
            val NAMES =
                listOf(
                    "Rina Kartika",
                    "Bagus Pratama",
                    "Salsa Nabila",
                    "Dimas Aryo",
                    "Putri Ayu",
                    "Fajar Nugroho",
                    "Nadia Rahma",
                    "Rizky Hidayat",
                    "Intan Permata",
                    "Yoga Saputra",
                    "Citra Lestari",
                    "Andi Wijaya",
                )
            val CATEGORIES = listOf(NamedOption(1, "TKA SMA"), NamedOption(2, "UTBK-SNBT"))
            val SUBJECTS =
                listOf(
                    NamedOption(10, "Matematika"),
                    NamedOption(11, "Penalaran Umum"),
                    NamedOption(12, "Literasi Bahasa Indonesia"),
                )
            val TAXONOMY =
                TaxonomyHierarchy(
                    branches =
                        listOf(
                            TaxonomyBranch(1, categoryId = 1, subjectId = 10, status = "active", orderNumber = 1),
                            TaxonomyBranch(2, categoryId = 2, subjectId = 11, status = "active", orderNumber = 1),
                            TaxonomyBranch(3, categoryId = 2, subjectId = 12, status = "active", orderNumber = 2),
                        ),
                )
            val TRYOUT_CARDS =
                listOf(
                    LeaderboardPackageCard(
                        11,
                        "Try Out UTBK-SNBT 1",
                        "UTBK-SNBT",
                        160,
                        195,
                        rank = 8,
                        totalParticipants = 64,
                        points = 612,
                        accuracy = 71,
                        change = 2,
                    ),
                    LeaderboardPackageCard(
                        WINDOWED_ID,
                        "Try Out Akbar Desember",
                        "UTBK-SNBT",
                        160,
                        195,
                        totalParticipants = 0,
                    ),
                    LeaderboardPackageCard(13, "Try Out TKA SMA Matematika", "TKA SMA", 40, 90, totalParticipants = 22),
                )
            val PRACTICE_CARDS =
                listOf(
                    LeaderboardPackageCard(
                        21,
                        "Latihan Aljabar",
                        "TKA SMA",
                        20,
                        timeLabel = "Tanpa batas waktu",
                        rank = 3,
                        totalParticipants = 18,
                        points = 92,
                        pointsLabel = "Nilai",
                        accuracy = 90,
                    ),
                    LeaderboardPackageCard(
                        22,
                        "Latihan Penalaran Umum",
                        "UTBK-SNBT",
                        30,
                        timeLabel = "45 Menit",
                        totalParticipants = 9,
                        pointsLabel = "Skor Latihan",
                    ),
                )
        }
    }
