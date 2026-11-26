package com.gotest.app.core.data.di

import com.gotest.app.core.data.hidden.HiddenExamRepository
import com.gotest.app.core.data.hidden.HiddenLeaderboardRepository
import com.gotest.app.core.data.hidden.HiddenStudyPackageRepository
import com.gotest.app.core.data.hidden.HiddenSubscriptionRepository
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: the T-018 catalog, runner, and Leaderboard (out of the graph, A2c-2) and Langganan (AppFeatures.PAYMENTS) have no sample data; Latihan, Try Out, Riwayat, and Leaderboard use their API repositories. */
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds fun bindStudyPackageRepository(impl: HiddenStudyPackageRepository): StudyPackageRepository

    @Binds fun bindExamRepository(impl: HiddenExamRepository): ExamRepository

    @Binds fun bindLeaderboardRepository(impl: HiddenLeaderboardRepository): LeaderboardRepository

    @Binds fun bindSubscriptionRepository(impl: HiddenSubscriptionRepository): SubscriptionRepository
}
