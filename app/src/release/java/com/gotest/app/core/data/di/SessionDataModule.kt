package com.gotest.app.core.data.di

import com.gotest.app.core.data.api.ApiAuthRepository
import com.gotest.app.core.data.api.ApiSignInOptionsRepository
import com.gotest.app.core.data.api.ApiSignUpRepository
import com.gotest.app.core.data.api.ApiUserRepository
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds sign in, sign up, read the account, and read the sign-in options through the API (T-026 A2a). */
@Module
@InstallIn(SingletonComponent::class)
interface SessionDataModule {
    @Binds fun bindAuthRepository(impl: ApiAuthRepository): AuthRepository

    @Binds fun bindUserRepository(impl: ApiUserRepository): UserRepository

    @Binds fun bindSignInOptionsRepository(impl: ApiSignInOptionsRepository): SignInOptionsRepository

    @Binds fun bindSignUpRepository(impl: ApiSignUpRepository): SignUpRepository
}
