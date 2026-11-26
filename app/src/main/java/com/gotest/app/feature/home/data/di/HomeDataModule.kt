package com.gotest.app.feature.home.data.di

import com.gotest.app.feature.home.data.FakeNotificationRepository
import com.gotest.app.feature.home.domain.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The Notifikasi inbox, hidden until the web has it (AppFeatures.NOTIFICATION_INBOX). Beranda and Cari are bound
 * in HomePagesModule (src/debug, src/release).
 */
@Module
@InstallIn(SingletonComponent::class)
interface HomeDataModule {
    @Binds fun bindNotificationRepository(impl: FakeNotificationRepository): NotificationRepository
}
