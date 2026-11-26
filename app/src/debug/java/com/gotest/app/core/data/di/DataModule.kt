package com.gotest.app.core.data.di

import com.gotest.app.core.data.fake.FakeExamRepository
import com.gotest.app.core.data.fake.FakeLeaderboardRepository
import com.gotest.app.core.data.fake.FakeStudyPackageRepository
import com.gotest.app.core.data.fake.FakeSubscriptionRepository
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Debug builds: the T-018 catalog, runner, and Leaderboard (out of the graph, A2c-2) and Langganan on sample
 * data. Release builds bind empty replacements in src/release (no sample data ships, guard `checkNoFakeData`).
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds fun bindStudyPackageRepository(impl: FakeStudyPackageRepository): StudyPackageRepository

    @Binds fun bindExamRepository(impl: FakeExamRepository): ExamRepository

    @Binds fun bindLeaderboardRepository(impl: FakeLeaderboardRepository): LeaderboardRepository

    @Binds fun bindSubscriptionRepository(impl: FakeSubscriptionRepository): SubscriptionRepository
}
