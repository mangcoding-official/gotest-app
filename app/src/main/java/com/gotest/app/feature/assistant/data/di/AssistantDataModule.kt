package com.gotest.app.feature.assistant.data.di

import com.gotest.app.feature.assistant.data.ContentAttachmentReader
import com.gotest.app.feature.assistant.data.FakeAssistantRepository
import com.gotest.app.feature.assistant.domain.AssistantRepository
import com.gotest.app.feature.assistant.domain.AttachmentReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AssistantDataModule {
    @Binds fun bindAssistantRepository(impl: FakeAssistantRepository): AssistantRepository

    @Binds fun bindAttachmentReader(impl: ContentAttachmentReader): AttachmentReader
}
