package com.gotest.app.feature.ruang.data.di

import android.os.SystemClock
import com.gotest.app.feature.ruang.data.ElapsedClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ElapsedClockModule {
    @Provides
    fun provideElapsedClock(): ElapsedClock = ElapsedClock { SystemClock.elapsedRealtime() }
}
