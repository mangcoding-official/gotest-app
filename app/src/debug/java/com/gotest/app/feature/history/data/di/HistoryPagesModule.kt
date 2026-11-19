package com.gotest.app.feature.history.data.di

import com.gotest.app.feature.history.data.FakeHistoryPagesRepository
import com.gotest.app.feature.history.domain.HistoryPagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: the fake Riwayat in src/debug (T-026 criterion 8). */
@Module
@InstallIn(SingletonComponent::class)
interface HistoryPagesModule {
    @Binds fun bindHistoryPagesRepository(impl: FakeHistoryPagesRepository): HistoryPagesRepository
}
