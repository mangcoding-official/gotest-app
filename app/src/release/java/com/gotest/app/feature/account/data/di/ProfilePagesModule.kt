package com.gotest.app.feature.account.data.di

import com.gotest.app.feature.account.data.ApiProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: Profil from API module F. */
@Module
@InstallIn(SingletonComponent::class)
interface ProfilePagesModule {
    @Binds fun bindProfilePagesRepository(impl: ApiProfilePagesRepository): ProfilePagesRepository
}
