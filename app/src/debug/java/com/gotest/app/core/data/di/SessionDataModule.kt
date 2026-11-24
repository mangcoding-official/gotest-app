package com.gotest.app.core.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.core.data.api.ApiAuthRepository
import com.gotest.app.core.data.api.ApiSignInOptionsRepository
import com.gotest.app.core.data.api.ApiSignUpRepository
import com.gotest.app.core.data.api.ApiUserRepository
import com.gotest.app.core.data.fake.FakeAuthRepository
import com.gotest.app.core.data.fake.FakeUserRepository
import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.auth.data.FakeSignInOptionsRepository
import com.gotest.app.feature.auth.data.FakeSignUpRepository
import com.gotest.app.feature.auth.domain.SignInOptionsRepository
import com.gotest.app.feature.auth.domain.SignUpRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/** Debug builds: the fake account, or signing in to the API with `gotest.useApi=true` (T-026 A2a). */
@Module
@InstallIn(SingletonComponent::class)
object SessionDataModule {
    @Provides
    fun provideAuthRepository(
        fake: Provider<FakeAuthRepository>,
        api: Provider<ApiAuthRepository>,
    ): AuthRepository = if (BuildConfig.USE_API) api.get() else fake.get()

    @Provides
    fun provideUserRepository(
        fake: Provider<FakeUserRepository>,
        api: Provider<ApiUserRepository>,
    ): UserRepository = if (BuildConfig.USE_API) api.get() else fake.get()

    @Provides
    fun provideSignInOptionsRepository(
        fake: Provider<FakeSignInOptionsRepository>,
        api: Provider<ApiSignInOptionsRepository>,
    ): SignInOptionsRepository = if (BuildConfig.USE_API) api.get() else fake.get()

    @Provides
    fun provideSignUpRepository(
        fake: Provider<FakeSignUpRepository>,
        api: Provider<ApiSignUpRepository>,
    ): SignUpRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
