package com.gotest.app.feature.leaderboard.data.di

import com.gotest.app.feature.leaderboard.data.FakeLeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake Leaderboard in src/debug (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface LeaderboardPagesModule {
    @Binds fun bindLeaderboardPagesRepository(impl: FakeLeaderboardPagesRepository): LeaderboardPagesRepository
}
