package com.gotest.app.feature.home.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.home.data.ApiDashboardRepository
import com.gotest.app.feature.home.data.ApiSearchRepository
import com.gotest.app.feature.home.data.FakeDashboardRepository
import com.gotest.app.feature.home.data.FakeSearchRepository
import com.gotest.app.feature.home.domain.DashboardRepository
import com.gotest.app.feature.home.domain.SearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the sample Beranda and Cari in src/debug, or the API with `gotest.useApi=true` (T-026 A2c-1).
 * Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object HomePagesModule {
    @Provides
    fun provideDashboardRepository(
        fake: Provider<FakeDashboardRepository>,
        api: Provider<ApiDashboardRepository>,
    ): DashboardRepository = if (BuildConfig.USE_API) api.get() else fake.get()

    @Provides
    fun provideSearchRepository(
        fake: Provider<FakeSearchRepository>,
        api: Provider<ApiSearchRepository>,
    ): SearchRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
