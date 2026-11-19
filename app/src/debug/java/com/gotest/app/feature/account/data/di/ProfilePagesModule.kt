package com.gotest.app.feature.account.data.di

import com.gotest.app.feature.account.data.FakeProfilePagesRepository
import com.gotest.app.feature.account.domain.ProfilePagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake Profil in src/debug (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface ProfilePagesModule {
    @Binds fun bindProfilePagesRepository(impl: FakeProfilePagesRepository): ProfilePagesRepository
}
