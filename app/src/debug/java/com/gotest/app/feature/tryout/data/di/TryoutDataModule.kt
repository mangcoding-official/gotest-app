package com.gotest.app.feature.tryout.data.di

import com.gotest.app.feature.tryout.data.FakeTryoutRepository
import com.gotest.app.feature.tryout.domain.TryoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake in src/debug (T-026 criterion 8).. */
@Module
@InstallIn(SingletonComponent::class)
interface TryoutDataModule {
    @Binds fun bindTryoutRepository(impl: FakeTryoutRepository): TryoutRepository
}
