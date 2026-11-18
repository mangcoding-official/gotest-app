package com.gotest.app.feature.leaderboard.ui.leaderboard

import com.gotest.app.feature.leaderboard.domain.LeaderboardEntry
import com.gotest.app.feature.leaderboard.domain.LeaderboardFilters
import com.gotest.app.feature.leaderboard.domain.LeaderboardOptions
import com.gotest.app.feature.leaderboard.domain.LeaderboardPackageCard
import com.gotest.app.feature.leaderboard.domain.LeaderboardPage
import com.gotest.app.feature.leaderboard.domain.LeaderboardScope
import com.gotest.app.feature.leaderboard.domain.NamedOption

/** Preview data only; the app's boards come from the server. */
internal object LeaderboardPreviewData {
    private val entries =
        listOf(
            LeaderboardEntry(1, 1, "Nadia Putri", 3, 912, 0),
            LeaderboardEntry(2, 2, "Rafi Akbar", 3, 880, 2),
            LeaderboardEntry(3, 3, "Salsa Aulia", 2, 861, -1),
            LeaderboardEntry(4, 4, "Alya Rahma", 2, 840, null),
            LeaderboardEntry(5, 5, "Gilang Ramadhan", 1, 820, 1, isCurrentUser = true),
        )

    val global =
        LeaderboardPage(
            options =
                LeaderboardOptions(
                    categories = listOf(NamedOption(1, "TKA SMA"), NamedOption(2, "UTBK-SNBT")),
                    subjects = listOf(NamedOption(3, "Penalaran Umum")),
                ),
            top = entries.take(3),
            rows = entries.drop(3),
            current = entries.last(),
            totalParticipants = 1_234,
            hasMore = true,
            updatedAt = "2026-11-18T07:05:00+00:00",
        )

    val packages =
        global.copy(
            filters = LeaderboardFilters(scope = LeaderboardScope.Package),
            packageCards =
                listOf(
                    LeaderboardPackageCard(
                        11,
                        "Try Out UTBK-SNBT 3",
                        "UTBK-SNBT",
                        160,
                        195,
                        rank = 42,
                        totalParticipants = 137,
                        points = 612,
                        accuracy = 71,
                        change = 3,
                    ),
                    LeaderboardPackageCard(12, "Try Out TKA SMA 1", "TKA SMA", 120, 150, totalParticipants = 12),
                ),
        )
}
