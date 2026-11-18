package com.gotest.app.feature.support.data.di

import com.gotest.app.feature.support.data.FakeSupportRepository
import com.gotest.app.feature.support.domain.SupportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake Bantuan in src/debug (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface SupportModule {
    @Binds fun bindSupportRepository(impl: FakeSupportRepository): SupportRepository
}
