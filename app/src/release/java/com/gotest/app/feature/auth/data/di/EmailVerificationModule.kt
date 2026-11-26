package com.gotest.app.feature.auth.data.di

import com.gotest.app.feature.auth.data.ApiEmailVerificationRepository
import com.gotest.app.feature.auth.domain.EmailVerificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds always talk to the API (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface EmailVerificationModule {
    @Binds fun bindEmailVerificationRepository(impl: ApiEmailVerificationRepository): EmailVerificationRepository
}
