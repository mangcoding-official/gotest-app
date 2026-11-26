package com.gotest.app.feature.account.data.di

import com.gotest.app.core.data.hidden.HiddenAccountDataRepository
import com.gotest.app.core.data.hidden.HiddenSettingsRepository
import com.gotest.app.feature.account.domain.AccountDataRepository
import com.gotest.app.feature.account.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: Pengaturan and Data Pribadi are hidden (A2e-8); no sample data ships. */
@Module
@InstallIn(SingletonComponent::class)
interface AccountSettingsModule {
    @Binds fun bindSettingsRepository(impl: HiddenSettingsRepository): SettingsRepository

    @Binds fun bindAccountDataRepository(impl: HiddenAccountDataRepository): AccountDataRepository
}
