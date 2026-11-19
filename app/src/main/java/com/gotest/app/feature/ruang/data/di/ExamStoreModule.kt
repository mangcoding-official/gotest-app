package com.gotest.app.feature.ruang.data.di

import android.content.Context
import com.gotest.app.core.session.KeystoreTokenCipher
import com.gotest.app.core.session.SharedPreferencesStore
import com.gotest.app.feature.ruang.data.ExamLocalStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExamStoreModule {
    /** `gotest_exam` is excluded from backups like `gotest_session`: its tokens only decrypt on this phone. */
    @Provides
    @Singleton
    fun provideExamLocalStore(
        @ApplicationContext context: Context,
        json: Json,
    ): ExamLocalStore =
        ExamLocalStore(
            store = SharedPreferencesStore(context.getSharedPreferences("gotest_exam", Context.MODE_PRIVATE)),
            cipher = KeystoreTokenCipher(),
            json = json,
        )
}
