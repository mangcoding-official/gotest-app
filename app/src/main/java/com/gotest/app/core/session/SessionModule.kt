package com.gotest.app.core.session

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context,
    ): SessionStore =
        SessionStore(
            store = SharedPreferencesStore(context.getSharedPreferences("gotest_session", Context.MODE_PRIVATE)),
            cipher = KeystoreTokenCipher(),
        )

    @Provides
    @Singleton
    fun provideSignUpDraftStore(
        @ApplicationContext context: Context,
    ): SignUpDraftStore =
        SignUpDraftStore(SharedPreferencesStore(context.getSharedPreferences("gotest_signup", Context.MODE_PRIVATE)))
}
