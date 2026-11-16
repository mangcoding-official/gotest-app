package com.gotest.app.feature.auth.data.di

import com.gotest.app.feature.auth.data.FakeAuthFlowRepository
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AuthDataModule {
    @Binds fun bindAuthFlowRepository(impl: FakeAuthFlowRepository): AuthFlowRepository
}
