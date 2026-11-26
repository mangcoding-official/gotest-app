package com.gotest.app.feature.assistant.data.di

import com.gotest.app.feature.assistant.data.ContentAttachmentReader
import com.gotest.app.feature.assistant.domain.AttachmentReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Files attached in Asisten. The conversation itself: AssistantRepositoryModule (src/debug, src/release). */
@Module
@InstallIn(SingletonComponent::class)
interface AssistantDataModule {
    @Binds fun bindAttachmentReader(impl: ContentAttachmentReader): AttachmentReader
}
