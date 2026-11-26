package com.gotest.app.feature.practice.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.practice.data.ApiPracticeRepository
import com.gotest.app.feature.practice.data.FakePracticeRepository
import com.gotest.app.feature.practice.domain.PracticeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake Latihan in src/debug (T-026 criterion 8), or the API with `gotest.useApi=true` to try a
 * debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object PracticeDataModule {
    @Provides
    fun providePracticeRepository(
        fake: Provider<FakePracticeRepository>,
        api: Provider<ApiPracticeRepository>,
    ): PracticeRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
