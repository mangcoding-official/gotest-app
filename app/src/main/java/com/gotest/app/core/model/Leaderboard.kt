package com.gotest.app.core.model

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val avatarUrl: String?,
    val points: Int,
    val attempts: Int?,
    /** Finish time on a package leaderboard, in seconds. */
    val finishSeconds: Int?,
    /** Positions gained (+) or lost (-) since the last update. */
    val rankChange: Int,
    val isCurrentUser: Boolean,
)

data class GlobalLeaderboard(
    val myRank: Int,
    val myRankChange: Int,
    val totalUsers: Int,
    val entries: List<LeaderboardEntry>,
)

/** "Per Paket" card: how the student ranks in one finished tryout. */
data class PackageRanking(
    val packageId: String,
    val title: String,
    val examLabel: String,
    val questionCount: Int,
    val durationMinutes: Int,
    val rank: Int,
    val participants: Int,
    val points: Int,
    val accuracyPercent: Int,
    val rankChange: Int,
)

data class PackageLeaderboard(
    val packageId: String,
    val title: String,
    val examLabel: String,
    val questionCount: Int,
    val durationMinutes: Int,
    val entries: List<LeaderboardEntry>,
)
