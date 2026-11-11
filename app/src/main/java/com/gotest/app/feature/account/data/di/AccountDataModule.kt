package com.gotest.app.feature.account.data.di

import com.gotest.app.feature.account.data.FakeHelpRepository
import com.gotest.app.feature.account.data.FakeSettingsRepository
import com.gotest.app.feature.account.domain.HelpRepository
import com.gotest.app.feature.account.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Account-only repositories. Point these at remote implementations once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface AccountDataModule {
    @Binds fun bindSettingsRepository(impl: FakeSettingsRepository): SettingsRepository

    @Binds fun bindHelpRepository(impl: FakeHelpRepository): HelpRepository
}
