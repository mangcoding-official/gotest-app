package com.gotest.app.feature.studypackage.data.di

import com.gotest.app.feature.studypackage.data.FakeSavedPackageRepository
import com.gotest.app.feature.studypackage.domain.SavedPackageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Paket-only repositories. Point these at remote implementations once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyPackageDataModule {
    @Binds fun bindSavedPackageRepository(impl: FakeSavedPackageRepository): SavedPackageRepository
}
