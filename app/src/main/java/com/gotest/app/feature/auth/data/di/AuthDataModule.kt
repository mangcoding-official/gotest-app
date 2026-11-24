package com.gotest.app.feature.auth.data.di

import com.gotest.app.feature.auth.data.FakeAuthFlowRepository
import com.gotest.app.feature.auth.data.FakeEmailCodeSignInRepository
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.EmailCodeSignInRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AuthDataModule {
    @Binds fun bindAuthFlowRepository(impl: FakeAuthFlowRepository): AuthFlowRepository

    @Binds fun bindEmailCodeSignInRepository(impl: FakeEmailCodeSignInRepository): EmailCodeSignInRepository
}
