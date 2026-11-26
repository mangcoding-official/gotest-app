package com.gotest.app.feature.history.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.history.data.ApiHistoryPagesRepository
import com.gotest.app.feature.history.data.FakeHistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake Riwayat in src/debug (T-026 criterion 8), or the API with `gotest.useApi=true` to try a
 * debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object HistoryPagesModule {
    @Provides
    fun provideHistoryPagesRepository(
        fake: Provider<FakeHistoryPagesRepository>,
        api: Provider<ApiHistoryPagesRepository>,
    ): HistoryPagesRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
