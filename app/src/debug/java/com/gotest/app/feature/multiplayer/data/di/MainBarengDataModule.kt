package com.gotest.app.feature.multiplayer.data.di

import com.gotest.app.feature.multiplayer.data.FakeMainBarengRepository
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake in src/debug (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface MainBarengDataModule {
    @Binds fun bindMainBarengRepository(impl: FakeMainBarengRepository): MainBarengRepository
}
