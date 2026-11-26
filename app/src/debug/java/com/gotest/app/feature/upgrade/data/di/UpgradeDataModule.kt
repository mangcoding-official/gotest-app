package com.gotest.app.feature.upgrade.data.di

import com.gotest.app.feature.upgrade.data.FakePromoRepository
import com.gotest.app.feature.upgrade.domain.PromoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: a sample promo (hidden with payments, AppFeatures.PAYMENTS); release has none. */
@Module
@InstallIn(SingletonComponent::class)
interface UpgradeDataModule {
    @Binds fun bindPromoRepository(impl: FakePromoRepository): PromoRepository
}
