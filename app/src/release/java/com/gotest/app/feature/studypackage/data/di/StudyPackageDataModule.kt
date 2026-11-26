package com.gotest.app.feature.studypackage.data.di

import com.gotest.app.core.data.hidden.HiddenSavedPackageRepository
import com.gotest.app.feature.studypackage.domain.SavedPackageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: Paket Tersimpan is hidden (AppFeatures.SAVED_PACKAGES) and empty. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyPackageDataModule {
    @Binds fun bindSavedPackageRepository(impl: HiddenSavedPackageRepository): SavedPackageRepository
}
