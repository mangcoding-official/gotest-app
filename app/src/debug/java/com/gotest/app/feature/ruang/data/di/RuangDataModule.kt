package com.gotest.app.feature.ruang.data.di

import com.gotest.app.feature.ruang.data.FakeExamRepository
import com.gotest.app.feature.ruang.data.FakeRuangRepository
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.RuangRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fakes in src/debug (T-026 criterion 8). Release builds bind the API in src/release. */
@Module
@InstallIn(SingletonComponent::class)
interface RuangDataModule {
    @Binds fun bindRuangRepository(impl: FakeRuangRepository): RuangRepository

    @Binds fun bindExamRepository(impl: FakeExamRepository): ExamRepository
}
