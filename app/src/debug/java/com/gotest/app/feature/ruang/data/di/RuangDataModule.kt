package com.gotest.app.feature.ruang.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.ruang.data.ApiExamRepository
import com.gotest.app.feature.ruang.data.ApiRuangRepository
import com.gotest.app.feature.ruang.data.ApiStudyPlanRepository
import com.gotest.app.feature.ruang.data.FakeExamRepository
import com.gotest.app.feature.ruang.data.FakeRuangRepository
import com.gotest.app.feature.ruang.data.FakeStudyPlanRepository
import com.gotest.app.feature.ruang.domain.ExamRepository
import com.gotest.app.feature.ruang.domain.RuangRepository
import com.gotest.app.feature.ruang.domain.StudyPlanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake Ruang Mitra, Ujian, and Rencana Belajar in src/debug (T-026 criterion 8), or the API
 * with `gotest.useApi=true` to try a debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object RuangDataModule {
    @Provides
    fun provideRuangRepository(
        fake: Provider<FakeRuangRepository>,
        api: Provider<ApiRuangRepository>,
    ): RuangRepository = if (BuildConfig.USE_API) api.get() else fake.get()

    @Provides
    fun provideExamRepository(
        fake: Provider<FakeExamRepository>,
        api: Provider<ApiExamRepository>,
    ): ExamRepository = if (BuildConfig.USE_API) api.get() else fake.get()

    @Provides
    fun provideStudyPlanRepository(
        fake: Provider<FakeStudyPlanRepository>,
        api: Provider<ApiStudyPlanRepository>,
    ): StudyPlanRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
