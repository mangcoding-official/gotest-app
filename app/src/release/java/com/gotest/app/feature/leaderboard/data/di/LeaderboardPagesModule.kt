package com.gotest.app.feature.leaderboard.data.di

import com.gotest.app.feature.leaderboard.data.ApiLeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always read the Leaderboard from the API (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface LeaderboardPagesModule {
    @Binds fun bindLeaderboardPagesRepository(impl: ApiLeaderboardPagesRepository): LeaderboardPagesRepository
}
