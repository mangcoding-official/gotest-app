package com.gotest.app.feature.support.data.di

import com.gotest.app.feature.support.data.ApiSupportRepository
import com.gotest.app.feature.support.domain.SupportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: Bantuan from API module F. */
@Module
@InstallIn(SingletonComponent::class)
interface SupportModule {
    @Binds fun bindSupportRepository(impl: ApiSupportRepository): SupportRepository
}
