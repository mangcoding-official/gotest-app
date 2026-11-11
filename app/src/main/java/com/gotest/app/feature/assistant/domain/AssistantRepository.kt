package com.gotest.app.feature.assistant.domain

import kotlinx.coroutines.flow.Flow

/** The "Asisten Gotest" conversation. Bound to a fake until the assistant API exists. */
interface AssistantRepository {
    /** The current conversation, oldest message first; empty before the student asks anything. */
    fun observeMessages(): Flow<List<AssistantMessage>>

    /**
     * Adds the student's message and suspends until the assistant has answered.
     * [voiceDurationSeconds] marks a message dictated through the voice overlay.
     */
    suspend fun sendMessage(
        text: String,
        voiceDurationSeconds: Int? = null,
    ): Result<Unit>

    /** Live speech-to-text transcript, growing as the student speaks, until the flow is cancelled. */
    fun listen(): Flow<String>
}
