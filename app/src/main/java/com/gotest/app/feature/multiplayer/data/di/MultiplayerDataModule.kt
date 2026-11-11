package com.gotest.app.feature.multiplayer.data.di

import com.gotest.app.feature.multiplayer.data.FakeMultiplayerRepository
import com.gotest.app.feature.multiplayer.domain.MultiplayerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Main Bareng data. Point this at a realtime implementation once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface MultiplayerDataModule {
    @Binds fun bindMultiplayerRepository(impl: FakeMultiplayerRepository): MultiplayerRepository
}
