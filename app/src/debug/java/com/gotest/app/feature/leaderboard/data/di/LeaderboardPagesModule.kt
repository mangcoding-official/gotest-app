package com.gotest.app.feature.leaderboard.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.leaderboard.data.ApiLeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.data.FakeLeaderboardPagesRepository
import com.gotest.app.feature.leaderboard.domain.LeaderboardPagesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake Leaderboard in src/debug (T-026 criterion 8), or the API with `gotest.useApi=true` to try a
 * debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object LeaderboardPagesModule {
    @Provides
    fun provideLeaderboardPagesRepository(
        fake: Provider<FakeLeaderboardPagesRepository>,
        api: Provider<ApiLeaderboardPagesRepository>,
    ): LeaderboardPagesRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
