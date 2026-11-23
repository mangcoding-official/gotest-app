package com.gotest.app.feature.tryout.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.tryout.data.ApiTryoutRepository
import com.gotest.app.feature.tryout.data.FakeTryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake Try Out in src/debug (T-026 criterion 8), or the API with `gotest.useApi=true` to try a
 * debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object TryoutDataModule {
    @Provides
    fun provideTryoutRepository(
        fake: Provider<FakeTryoutRepository>,
        api: Provider<ApiTryoutRepository>,
    ): TryoutRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
