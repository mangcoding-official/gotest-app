package com.gotest.app.feature.upgrade.data.di

import com.gotest.app.core.data.hidden.HiddenPromoRepository
import com.gotest.app.feature.upgrade.domain.PromoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: promos are hidden with payments (AppFeatures.PAYMENTS); none exists. */
@Module
@InstallIn(SingletonComponent::class)
interface UpgradeDataModule {
    @Binds fun bindPromoRepository(impl: HiddenPromoRepository): PromoRepository
}
