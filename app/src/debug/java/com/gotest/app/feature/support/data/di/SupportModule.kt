package com.gotest.app.feature.support.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.support.data.ApiSupportRepository
import com.gotest.app.feature.support.data.FakeSupportRepository
import com.gotest.app.feature.support.domain.SupportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake Bantuan in src/debug (T-026 criterion 8), or the API with `gotest.useApi=true` to try a
 * debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupportModule {
    @Provides
    fun provideSupportRepository(
        fake: Provider<FakeSupportRepository>,
        api: Provider<ApiSupportRepository>,
    ): SupportRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
