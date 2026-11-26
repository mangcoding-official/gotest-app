package com.gotest.app.feature.account.data.di

import com.gotest.app.BuildConfig
import com.gotest.app.feature.account.data.ApiProfilePagesRepository
import com.gotest.app.feature.account.data.FakeProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

/**
 * Debug builds: the fake the Profil in src/debug (T-026 criterion 8), or the API with `gotest.useApi=true` to try a
 * debug build against staging. Release builds bind the API in src/release.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfilePagesModule {
    @Provides
    fun provideProfilePagesRepository(
        fake: Provider<FakeProfilePagesRepository>,
        api: Provider<ApiProfilePagesRepository>,
    ): ProfilePagesRepository = if (BuildConfig.USE_API) api.get() else fake.get()
}
