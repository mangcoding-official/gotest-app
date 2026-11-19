package com.gotest.app.feature.history.data.di

import com.gotest.app.feature.history.data.HistoryCsvSaver
import com.gotest.app.feature.history.data.HistoryCsvStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** "Export data" of Riwayat saves to Downloads in every build. */
@Module
@InstallIn(SingletonComponent::class)
interface HistoryCsvModule {
    @Binds fun bindHistoryCsvStore(impl: HistoryCsvSaver): HistoryCsvStore
}
