package com.gotest.app.feature.assistant.domain

import kotlinx.coroutines.flow.Flow

/** The "Asisten Gotest" conversations. Bound to a fake until the assistant API exists. */
interface AssistantRepository {
    /** The conversation on screen; a fresh, empty one until the student asks something. */
    fun observeCurrentConversation(): Flow<AssistantConversation>

    /** Conversations that have messages, the most recent first. */
    fun observeHistory(): Flow<List<ConversationSummary>>

    /**
     * Adds the student's message to the current conversation and suspends until the assistant
     * has answered. [voiceDurationSeconds] marks a message dictated through the voice overlay.
     * The text may be empty when the message carries [attachments].
     */
    suspend fun sendMessage(
        text: String,
        voiceDurationSeconds: Int? = null,
        attachments: List<AssistantAttachment> = emptyList(),
    ): Result<Unit>

    /** "Percakapan baru": switches to an empty conversation. */
    fun startNewConversation()

    /** Switches to a conversation from the history. */
    fun openConversation(conversationId: String)

    /** "Hapus semua riwayat": deletes every conversation and starts a new one. */
    suspend fun clearHistory(): Result<Unit>

    /** Live speech-to-text transcript, growing as the student speaks, until the flow is cancelled. */
    fun listen(): Flow<String>
}
