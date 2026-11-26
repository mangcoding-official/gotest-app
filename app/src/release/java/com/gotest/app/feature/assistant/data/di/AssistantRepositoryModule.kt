package com.gotest.app.feature.assistant.data.di

import com.gotest.app.core.data.hidden.HiddenAssistantRepository
import com.gotest.app.feature.assistant.domain.AssistantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release builds: Asisten AI is hidden (AppFeatures.ASSISTANT); no sample conversations ship. */
@Module
@InstallIn(SingletonComponent::class)
interface AssistantRepositoryModule {
    @Binds fun bindAssistantRepository(impl: HiddenAssistantRepository): AssistantRepository
}
