package com.gotest.app.feature.assistant.data.di

import com.gotest.app.feature.assistant.data.FakeAssistantRepository
import com.gotest.app.feature.assistant.domain.AssistantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds: Asisten AI on sample conversations (hidden, AppFeatures.ASSISTANT). */
@Module
@InstallIn(SingletonComponent::class)
interface AssistantRepositoryModule {
    @Binds fun bindAssistantRepository(impl: FakeAssistantRepository): AssistantRepository
}
