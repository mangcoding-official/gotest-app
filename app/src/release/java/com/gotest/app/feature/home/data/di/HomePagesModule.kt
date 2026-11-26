package com.gotest.app.feature.home.data.di

import com.gotest.app.feature.home.data.ApiDashboardRepository
import com.gotest.app.feature.home.data.ApiSearchRepository
import com.gotest.app.feature.home.domain.DashboardRepository
import com.gotest.app.feature.home.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always talk to the API for Beranda and Cari (T-026 A2c-1). */
@Module
@InstallIn(SingletonComponent::class)
interface HomePagesModule {
    @Binds fun bindDashboardRepository(impl: ApiDashboardRepository): DashboardRepository

    @Binds fun bindSearchRepository(impl: ApiSearchRepository): SearchRepository
}
