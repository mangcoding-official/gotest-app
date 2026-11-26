package com.gotest.app.feature.studypackage.data.di

import com.gotest.app.feature.studypackage.data.FakeSavedPackageRepository
import com.gotest.app.feature.studypackage.domain.SavedPackageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: Paket Tersimpan on sample data (hidden, AppFeatures.SAVED_PACKAGES); release binds an empty one. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyPackageDataModule {
    @Binds fun bindSavedPackageRepository(impl: FakeSavedPackageRepository): SavedPackageRepository
}
