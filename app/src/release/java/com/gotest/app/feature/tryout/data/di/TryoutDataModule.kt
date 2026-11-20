package com.gotest.app.feature.tryout.data.di

import com.gotest.app.feature.tryout.data.ApiTryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always talk to the API (T-026 criterion 8).. */
@Module
@InstallIn(SingletonComponent::class)
interface TryoutDataModule {
    @Binds fun bindTryoutRepository(impl: ApiTryoutRepository): TryoutRepository
}
