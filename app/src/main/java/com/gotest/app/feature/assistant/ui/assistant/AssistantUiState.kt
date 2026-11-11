package com.gotest.app.feature.assistant.ui.assistant

import androidx.annotation.StringRes
import com.gotest.app.feature.assistant.domain.AssistantMessage
import java.time.LocalDate

/** The voice overlay (frame 12266) while it is open. */
data class VoiceInputState(
    val transcript: String = "",
    val elapsedSeconds: Int = 0,
)

/** One row of the conversation list. */
sealed interface AssistantChatItem {
    val key: String

    /** "Hari ini" pill above the first message of each day. */
    data class DayDivider(
        val date: LocalDate,
    ) : AssistantChatItem {
        override val key: String get() = "day_$date"
    }

    data class Message(
        val message: AssistantMessage,
    ) : AssistantChatItem {
        override val key: String get() = message.id
    }

    /** The three dots while the assistant prepares an answer. */
    data object Typing : AssistantChatItem {
        override val key: String get() = "typing"
    }
}

sealed interface AssistantUiState {
    data object Loading : AssistantUiState

    data object Error : AssistantUiState

    /** No messages yet: the welcome layout with suggestion cards (frame 11855). */
    data class Empty(
        val firstName: String,
        val voice: VoiceInputState?,
    ) : AssistantUiState

    /** The conversation (frame 11990). */
    data class Success(
        val items: List<AssistantChatItem>,
        val voice: VoiceInputState?,
    ) : AssistantUiState
}

sealed interface AssistantUiEvent {
    data class InputChange(
        val value: String,
    ) : AssistantUiEvent

    /** Sends what is in the input. */
    data object Send : AssistantUiEvent

    /** A suggestion card or prompt chip: sends its text straight away. */
    data class SendPrompt(
        val text: String,
    ) : AssistantUiEvent

    data object StartVoice : AssistantUiEvent

    /** "Selesai": sends the transcript as a voice message. */
    data object FinishVoice : AssistantUiEvent

    /** "Batal", the close button or system Back. */
    data object CancelVoice : AssistantUiEvent

    /** "Ketik": closes the overlay and focuses the input. */
    data object SwitchToKeyboard : AssistantUiEvent

    data object Retry : AssistantUiEvent
}

sealed interface AssistantEffect {
    data object FocusInput : AssistantEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : AssistantEffect
}
