package com.gotest.app.feature.home.data.di

import com.gotest.app.core.data.hidden.HiddenNotificationRepository
import com.gotest.app.feature.home.domain.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: the Notifikasi inbox is hidden (AppFeatures.NOTIFICATION_INBOX) and empty. */
@Module
@InstallIn(SingletonComponent::class)
interface HomeDataModule {
    @Binds fun bindNotificationRepository(impl: HiddenNotificationRepository): NotificationRepository
}
