package com.gotest.app.core.data.di

import com.gotest.app.core.data.api.ApiAuthFlowRepository
import com.gotest.app.core.data.api.ApiAuthRepository
import com.gotest.app.core.data.api.ApiEmailCodeSignInRepository
import com.gotest.app.core.data.api.ApiSignInOptionsRepository
import com.gotest.app.core.data.api.ApiSignUpRepository
import com.gotest.app.core.data.api.ApiUserRepository
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.auth.domain.AuthFlowRepository
import com.gotest.app.feature.auth.domain.EmailCodeSignInRepository
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds sign in (every way), sign up, and read the account and sign-in options through the API (T-026 A2a). */
@Module
@InstallIn(SingletonComponent::class)
interface SessionDataModule {
    @Binds fun bindAuthRepository(impl: ApiAuthRepository): AuthRepository

    @Binds fun bindUserRepository(impl: ApiUserRepository): UserRepository

    @Binds fun bindSignInOptionsRepository(impl: ApiSignInOptionsRepository): SignInOptionsRepository

    @Binds fun bindSignUpRepository(impl: ApiSignUpRepository): SignUpRepository

    @Binds fun bindAuthFlowRepository(impl: ApiAuthFlowRepository): AuthFlowRepository

    @Binds fun bindEmailCodeSignInRepository(impl: ApiEmailCodeSignInRepository): EmailCodeSignInRepository
}
