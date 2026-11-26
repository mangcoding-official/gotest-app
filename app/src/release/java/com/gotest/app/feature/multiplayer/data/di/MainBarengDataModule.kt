package com.gotest.app.feature.multiplayer.data.di

import com.gotest.app.feature.multiplayer.data.ApiMainBarengRepository
import com.gotest.app.feature.multiplayer.domain.MainBarengRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always talk to the API (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface MainBarengDataModule {
    @Binds fun bindMainBarengRepository(impl: ApiMainBarengRepository): MainBarengRepository
}
