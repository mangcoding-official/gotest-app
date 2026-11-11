package com.gotest.app.core.data.di

import com.gotest.app.core.data.fake.FakeAuthRepository
import com.gotest.app.core.data.fake.FakeExamRepository
import com.gotest.app.core.data.fake.FakeLeaderboardRepository
import com.gotest.app.core.data.fake.FakeStudyPackageRepository
import com.gotest.app.core.data.fake.FakeSubscriptionRepository
import com.gotest.app.core.data.fake.FakeUserRepository
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Shared repositories. Point these at remote implementations once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds fun bindUserRepository(impl: FakeUserRepository): UserRepository

    @Binds fun bindStudyPackageRepository(impl: FakeStudyPackageRepository): StudyPackageRepository

    @Binds fun bindExamRepository(impl: FakeExamRepository): ExamRepository

    @Binds fun bindLeaderboardRepository(impl: FakeLeaderboardRepository): LeaderboardRepository

    @Binds fun bindSubscriptionRepository(impl: FakeSubscriptionRepository): SubscriptionRepository
}
