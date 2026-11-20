package com.gotest.app.feature.practice.data.di

import com.gotest.app.feature.practice.data.FakePracticeRepository
import com.gotest.app.feature.practice.domain.PracticeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake in src/debug (T-026 criterion 8).. */
@Module
@InstallIn(SingletonComponent::class)
interface PracticeDataModule {
    @Binds fun bindPracticeRepository(impl: FakePracticeRepository): PracticeRepository
}
