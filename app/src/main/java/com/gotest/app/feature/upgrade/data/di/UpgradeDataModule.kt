package com.gotest.app.feature.upgrade.data.di

import com.gotest.app.feature.upgrade.data.FakePromoRepository
import com.gotest.app.feature.upgrade.domain.PromoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Upgrade-only repositories. Point these at remote implementations once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface UpgradeDataModule {
    @Binds fun bindPromoRepository(impl: FakePromoRepository): PromoRepository
}
