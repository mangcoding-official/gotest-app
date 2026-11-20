package com.gotest.app.feature.practice.data.di

import com.gotest.app.feature.practice.data.ApiPracticeRepository
import com.gotest.app.feature.practice.domain.PracticeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always talk to the API (T-026 criterion 8).. */
@Module
@InstallIn(SingletonComponent::class)
interface PracticeDataModule {
    @Binds fun bindPracticeRepository(impl: ApiPracticeRepository): PracticeRepository
}
