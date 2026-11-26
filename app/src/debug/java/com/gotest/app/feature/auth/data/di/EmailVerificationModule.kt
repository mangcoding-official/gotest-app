package com.gotest.app.feature.auth.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.auth.data.ApiEmailVerificationRepository
import com.gotest.app.feature.auth.data.FakeEmailVerificationRepository
import com.gotest.app.feature.auth.domain.EmailVerificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/** Debug builds: the fake in src/debug, or the API with `gotest.useApi=true` (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
object EmailVerificationModule {
    @Provides
    fun provideEmailVerificationRepository(
        fake: Provider<FakeEmailVerificationRepository>,
        api: Provider<ApiEmailVerificationRepository>,
    ): EmailVerificationRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
