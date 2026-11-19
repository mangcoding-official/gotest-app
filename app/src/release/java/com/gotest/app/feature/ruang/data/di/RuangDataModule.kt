package com.gotest.app.feature.ruang.data.di

import com.gotest.app.feature.ruang.data.ApiExamRepository
import com.gotest.app.feature.ruang.data.ApiRuangRepository
import com.gotest.app.feature.ruang.data.ApiStudyPlanRepository
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always talk to the API (T-026 criterion 8); debug builds bind fakes in src/debug. */
@Module
@InstallIn(SingletonComponent::class)
interface RuangDataModule {
    @Binds fun bindRuangRepository(impl: ApiRuangRepository): RuangRepository

    @Binds fun bindExamRepository(impl: ApiExamRepository): ExamRepository

    @Binds fun bindStudyPlanRepository(impl: ApiStudyPlanRepository): StudyPlanRepository
}
