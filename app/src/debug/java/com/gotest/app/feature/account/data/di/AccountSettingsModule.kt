package com.gotest.app.feature.account.data.di

import com.gotest.app.feature.account.data.FakeAccountDataRepository
import com.gotest.app.feature.account.data.FakeSettingsRepository
import com.gotest.app.feature.account.domain.AccountDataRepository
import com.gotest.app.feature.account.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: Pengaturan and Data Pribadi on sample data (hidden until the web has them, A2e-8). */
@Module
@InstallIn(SingletonComponent::class)
interface AccountSettingsModule {
    @Binds fun bindSettingsRepository(impl: FakeSettingsRepository): SettingsRepository

    @Binds fun bindAccountDataRepository(impl: FakeAccountDataRepository): AccountDataRepository
}
