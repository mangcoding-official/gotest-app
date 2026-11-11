package com.gotest.app.feature.assistant.ui.assistant

import androidx.annotation.StringRes
import com.gotest.app.feature.assistant.domain.AssistantAttachment
import com.gotest.app.feature.assistant.domain.AssistantMessage
import java.time.LocalDate
import java.time.LocalDateTime

/** The voice overlay (frame 12266) while it is open. */
data class VoiceInputState(
    val transcript: String = "",
    val elapsedSeconds: Int = 0,
)

/** Everything the composer holds besides the typed text. */
data class ComposerState(
    /** Files waiting to go out with the next message. */
    val attachments: List<AssistantAttachment> = emptyList(),
    /** "Tambah Lampiran" (frame 22009) is open. */
    val isAttachmentSheetOpen: Boolean = false,
    val voice: VoiceInputState? = null,
)

/** The three tiles of "Tambah Lampiran". */
enum class AttachmentSource { Camera, Gallery, Document }

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
        val composer: ComposerState,
    ) : AssistantUiState

    /** The conversation (frame 11990). */
    data class Success(
        val conversationId: String,
        val items: List<AssistantChatItem>,
        val composer: ComposerState,
    ) : AssistantUiState
}

/** The composer of the welcome or conversation layout; null while loading or failed. */
val AssistantUiState.composer: ComposerState?
    get() =
        when (this) {
            is AssistantUiState.Empty -> composer
            is AssistantUiState.Success -> composer
            else -> null
        }

/** Sections of "Riwayat Percakapan", newest first. */
enum class HistoryPeriod { Today, Yesterday, LastSevenDays, Older }

data class HistoryItem(
    val id: String,
    val title: String,
    val lastMessageAt: LocalDateTime,
    /** "Sedang dibuka": the conversation on screen, highlighted. */
    val isOpen: Boolean,
)

data class HistoryGroup(
    val period: HistoryPeriod,
    val items: List<HistoryItem>,
)

/** "AI Chat / Menu Riwayat Percakapan" (frame 21622) and its clear-all confirmation. */
data class ConversationHistoryUiState(
    val isOpen: Boolean = false,
    val isLoading: Boolean = true,
    /** Conversations matching the search, grouped by day. */
    val groups: List<HistoryGroup> = emptyList(),
    /** False when nothing is stored at all, as opposed to nothing matching the search. */
    val hasHistory: Boolean = false,
    val isConfirmingClear: Boolean = false,
)

sealed interface AssistantUiEvent {
    /** Events of "Tambah Lampiran" and the pending attachments. */
    sealed interface AttachmentEvent : AssistantUiEvent

    /** Events of the "Riwayat Percakapan" panel. */
    sealed interface HistoryEvent : AssistantUiEvent

    data class InputChange(
        val value: String,
    ) : AssistantUiEvent

    /** Sends what is in the input, with the pending attachments. */
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

    /** The "+" button of the composer. */
    data object OpenAttachmentSheet : AttachmentEvent

    /** "Batal", a tile (its picker takes over) or a swipe down. */
    data object DismissAttachmentSheet : AttachmentEvent

    /** Content URIs from the camera, photo picker or document picker. */
    data class AttachmentsPicked(
        val uris: List<String>,
    ) : AttachmentEvent

    data class RemoveAttachment(
        val uri: String,
    ) : AttachmentEvent

    /** The menu button. */
    data object OpenHistory : HistoryEvent

    /** The close button, the scrim or system Back. */
    data object CloseHistory : HistoryEvent

    data class HistoryQueryChange(
        val value: String,
    ) : HistoryEvent

    /** "Percakapan baru". */
    data object NewConversation : HistoryEvent

    data class OpenConversation(
        val conversationId: String,
    ) : HistoryEvent

    /** "Hapus semua riwayat": asks for confirmation first. */
    data object ClearHistory : HistoryEvent

    data object ConfirmClearHistory : HistoryEvent

    data object DismissClearHistory : HistoryEvent
}

sealed interface AssistantEffect {
    data object FocusInput : AssistantEffect

    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : AssistantEffect
}
