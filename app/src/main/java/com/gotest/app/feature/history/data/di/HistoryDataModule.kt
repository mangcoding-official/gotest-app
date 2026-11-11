package com.gotest.app.feature.history.data.di

import com.gotest.app.feature.history.data.AndroidHistoryExporter
import com.gotest.app.feature.history.data.FakeHistoryRepository
import com.gotest.app.feature.history.domain.HistoryExporter
import com.gotest.app.feature.history.domain.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Riwayat-only bindings. Point the repository at a remote implementation once the API exists. */
@Module
@InstallIn(SingletonComponent::class)
interface HistoryDataModule {
    @Binds fun bindHistoryRepository(impl: FakeHistoryRepository): HistoryRepository

    @Binds fun bindHistoryExporter(impl: AndroidHistoryExporter): HistoryExporter
}
