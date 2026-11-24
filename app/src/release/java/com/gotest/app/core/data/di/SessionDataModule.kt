package com.gotest.app.core.data.di

import com.gotest.app.core.data.api.ApiAuthRepository
import com.gotest.app.core.data.api.ApiUserRepository
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds sign in and read the account through the API (T-026 A2a). */
@Module
@InstallIn(SingletonComponent::class)
interface SessionDataModule {
    @Binds fun bindAuthRepository(impl: ApiAuthRepository): AuthRepository

    @Binds fun bindUserRepository(impl: ApiUserRepository): UserRepository
}
