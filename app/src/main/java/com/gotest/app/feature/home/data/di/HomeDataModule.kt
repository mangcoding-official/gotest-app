package com.gotest.app.feature.home.data.di

import com.gotest.app.feature.home.data.FakeNotificationRepository
import com.gotest.app.feature.home.data.FakeSearchRepository
import com.gotest.app.feature.home.domain.NotificationRepository
import com.gotest.app.feature.home.domain.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Home-only repositories. Point these at remote implementations once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface HomeDataModule {
    @Binds fun bindNotificationRepository(impl: FakeNotificationRepository): NotificationRepository

    @Binds fun bindSearchRepository(impl: FakeSearchRepository): SearchRepository
}
