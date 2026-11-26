package com.gotest.app.feature.account.data.di

import com.gotest.app.feature.account.data.BitmapProfilePhotoProcessor
import com.gotest.app.feature.account.domain.ProfilePhotoProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The photo of Edit Profil. Pengaturan and Data Pribadi: AccountSettingsModule (src/debug, src/release). */
@Module
@InstallIn(SingletonComponent::class)
interface AccountDataModule {
    @Binds fun bindProfilePhotoProcessor(impl: BitmapProfilePhotoProcessor): ProfilePhotoProcessor
}
