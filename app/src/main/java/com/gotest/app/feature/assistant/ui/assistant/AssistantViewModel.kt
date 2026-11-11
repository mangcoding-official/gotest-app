package com.gotest.app.feature.assistant.ui.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.assistant.domain.AssistantConversation
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantRepository
import com.gotest.app.feature.assistant.domain.AttachmentReader
import com.gotest.app.feature.assistant.domain.AttachmentRejection
import com.gotest.app.feature.assistant.domain.AttachmentRules
import com.gotest.app.feature.assistant.domain.ConversationSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L
private const val ONE_SECOND_MS = 1_000L

/** "7 hari terakhir" covers the days before yesterday up to a week back. */
private const val LAST_WEEK_DAYS = 7L

/** Open/closed parts of the history panel; the conversations come from the repository. */
private data class HistoryPanelState(
    val isOpen: Boolean = false,
    val isConfirmingClear: Boolean = false,
)

@HiltViewModel
class AssistantViewModel
    @Inject
    constructor(
        private val assistantRepository: AssistantRepository,
        private val attachmentReader: AttachmentReader,
        userRepository: UserRepository,
    ) : ViewModel() {
        /** Text field content; Compose state so typing stays in sync with the field. */
        var input by mutableStateOf("")
            private set

        /** "Cari percakapan…" field content, Compose state for the same reason as [input]. */
        var historyQuery by mutableStateOf("")
            private set

        private val refresh = MutableStateFlow(0)
        private val composer = MutableStateFlow(ComposerState())
        private val historyPanel = MutableStateFlow(HistoryPanelState())
        private var voiceJob: Job? = null

        private val _effects = Channel<AssistantEffect>(Channel.BUFFERED)
        val effects: Flow<AssistantEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<AssistantUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        userRepository.observeProfile().map { it.firstName }.distinctUntilChanged(),
                        assistantRepository.observeCurrentConversation(),
                        composer,
                        ::toUiState,
                    ).onStart { emit(AssistantUiState.Loading) }
                        .catch { emit(AssistantUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AssistantUiState.Loading)

        val history: StateFlow<ConversationHistoryUiState> =
            combine(
                observeStoredConversations(),
                snapshotFlow { historyQuery },
                historyPanel,
                ::toHistoryState,
            ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ConversationHistoryUiState())

        fun onEvent(event: AssistantUiEvent) {
            when (event) {
                is AssistantUiEvent.InputChange -> {
                    input = event.value
                }

                AssistantUiEvent.Send -> {
                    send(input)
                    input = ""
                }

                is AssistantUiEvent.SendPrompt -> {
                    send(event.text)
                }

                AssistantUiEvent.StartVoice -> {
                    startVoice()
                }

                AssistantUiEvent.FinishVoice -> {
                    finishVoice()
                }

                AssistantUiEvent.CancelVoice -> {
                    stopVoice()
                }

                AssistantUiEvent.SwitchToKeyboard -> {
                    stopVoice()
                    _effects.trySend(AssistantEffect.FocusInput)
                }

                AssistantUiEvent.Retry -> {
                    refresh.update { it + 1 }
                }

                is AssistantUiEvent.AttachmentEvent -> {
                    onAttachmentEvent(event)
                }

                is AssistantUiEvent.HistoryEvent -> {
                    onHistoryEvent(event)
                }
            }
        }

        private fun onAttachmentEvent(event: AssistantUiEvent.AttachmentEvent) {
            when (event) {
                AssistantUiEvent.OpenAttachmentSheet -> {
                    if (composer.value.attachments.size >= AttachmentRules.MAX_FILES) {
                        _effects.trySend(AssistantEffect.ShowMessage(AttachmentRejection.TooMany.messageRes))
                    } else {
                        composer.update { it.copy(isAttachmentSheetOpen = true) }
                    }
                }

                AssistantUiEvent.DismissAttachmentSheet -> {
                    composer.update { it.copy(isAttachmentSheetOpen = false) }
                }

                is AssistantUiEvent.AttachmentsPicked -> {
                    addAttachments(event.uris)
                }

                is AssistantUiEvent.RemoveAttachment -> {
                    composer.update { state ->
                        state.copy(attachments = state.attachments.filterNot { it.uri == event.uri })
                    }
                }
            }
        }

        private fun onHistoryEvent(event: AssistantUiEvent.HistoryEvent) {
            when (event) {
                AssistantUiEvent.OpenHistory -> {
                    // The voice overlay has its own menu button; the panel replaces it.
                    stopVoice()
                    historyPanel.update { it.copy(isOpen = true) }
                }

                AssistantUiEvent.CloseHistory -> {
                    closeHistory()
                }

                is AssistantUiEvent.HistoryQueryChange -> {
                    historyQuery = event.value
                }

                AssistantUiEvent.NewConversation -> {
                    assistantRepository.startNewConversation()
                    closeHistory()
                }

                is AssistantUiEvent.OpenConversation -> {
                    assistantRepository.openConversation(event.conversationId)
                    closeHistory()
                }

                AssistantUiEvent.ClearHistory -> {
                    historyPanel.update { it.copy(isConfirmingClear = true) }
                }

                AssistantUiEvent.DismissClearHistory -> {
                    historyPanel.update { it.copy(isConfirmingClear = false) }
                }

                AssistantUiEvent.ConfirmClearHistory -> {
                    clearHistory()
                }
            }
        }

        /** Sends [text] with whatever is attached; the attachments leave the composer right away. */
        private fun send(
            text: String,
            voiceDurationSeconds: Int? = null,
        ) {
            val message = text.trim()
            val attachments = composer.value.attachments
            if (message.isEmpty() && attachments.isEmpty()) return
            composer.update { it.copy(attachments = emptyList()) }
            viewModelScope.launch {
                assistantRepository
                    .sendMessage(message, voiceDurationSeconds, attachments)
                    .onFailure { _effects.send(AssistantEffect.ShowMessage(R.string.assistant_send_error)) }
            }
        }

        /** Reads what was picked and keeps what [AttachmentRules] allows; each broken rule gets a snackbar. */
        private fun addAttachments(uris: List<String>) {
            if (uris.isEmpty()) return
            viewModelScope.launch {
                val picked = uris.mapNotNull { attachmentReader.read(it) }
                if (picked.size < uris.size) {
                    _effects.send(AssistantEffect.ShowMessage(R.string.assistant_attachment_unreadable))
                }
                var rejections = emptySet<AttachmentRejection>()
                composer.update { state ->
                    val check = AttachmentRules.check(picked, state.attachments)
                    rejections = check.rejections
                    state.copy(attachments = state.attachments + check.accepted)
                }
                rejections.forEach { _effects.send(AssistantEffect.ShowMessage(it.messageRes)) }
            }
        }

        private fun clearHistory() {
            historyPanel.update { it.copy(isConfirmingClear = false) }
            viewModelScope.launch {
                assistantRepository
                    .clearHistory()
                    .onSuccess {
                        closeHistory()
                        _effects.send(AssistantEffect.ShowMessage(R.string.assistant_history_cleared))
                    }.onFailure { _effects.send(AssistantEffect.ShowMessage(R.string.assistant_history_clear_error)) }
            }
        }

        private fun closeHistory() {
            historyPanel.value = HistoryPanelState()
            historyQuery = ""
        }

        private fun startVoice() {
            if (voiceJob?.isActive == true) return
            composer.update { it.copy(voice = VoiceInputState()) }
            voiceJob =
                viewModelScope.launch {
                    launch {
                        while (isActive) {
                            delay(ONE_SECOND_MS)
                            updateVoice { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                        }
                    }
                    assistantRepository.listen().collect { transcript ->
                        updateVoice { it.copy(transcript = transcript) }
                    }
                }
        }

        private fun finishVoice() {
            val recording = composer.value.voice ?: return
            stopVoice()
            // Nothing heard: no voice message, even with attachments waiting.
            if (recording.transcript.isBlank()) return
            send(recording.transcript, voiceDurationSeconds = recording.elapsedSeconds.coerceAtLeast(1))
        }

        private fun stopVoice() {
            voiceJob?.cancel()
            voiceJob = null
            composer.update { it.copy(voice = null) }
        }

        private fun updateVoice(transform: (VoiceInputState) -> VoiceInputState) {
            composer.update { state -> state.copy(voice = state.voice?.let(transform)) }
        }

        private fun toUiState(
            firstName: String,
            conversation: AssistantConversation,
            composerState: ComposerState,
        ): AssistantUiState =
            if (conversation.messages.isEmpty()) {
                AssistantUiState.Empty(firstName = firstName, composer = composerState)
            } else {
                AssistantUiState.Success(
                    conversationId = conversation.id,
                    items = chatItems(conversation.messages, isReplying = conversation.isAwaitingReply),
                    composer = composerState,
                )
            }

        /** Messages with a day pill in front of each new day, plus the typing dots while waiting. */
        private fun chatItems(
            messages: List<AssistantMessage>,
            isReplying: Boolean,
        ): List<AssistantChatItem> =
            buildList {
                val zone = ZoneId.systemDefault()
                var lastDate: LocalDate? = null
                messages.forEach { message ->
                    val date = Instant.ofEpochMilli(message.sentAtEpochMillis).atZone(zone).toLocalDate()
                    if (date != lastDate) add(AssistantChatItem.DayDivider(date))
                    lastDate = date
                    add(AssistantChatItem.Message(message))
                }
                if (isReplying) add(AssistantChatItem.Typing)
            }

        /**
         * Null while the history loads. A failure reads as an empty history ("Belum ada riwayat");
         * the chat itself is unaffected.
         */
        private fun observeStoredConversations(): Flow<List<ConversationSummary>?> =
            assistantRepository
                .observeHistory()
                .onStart<List<ConversationSummary>?> { emit(null) }
                .catch { emit(emptyList()) }

        /** Filters by title and groups into "Hari ini", "Kemarin", "7 hari terakhir" and older. */
        private fun toHistoryState(
            conversations: List<ConversationSummary>?,
            query: String,
            panel: HistoryPanelState,
        ): ConversationHistoryUiState {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val search = query.trim()
            val groups =
                conversations
                    .orEmpty()
                    .filter { search.isEmpty() || it.title.contains(search, ignoreCase = true) }
                    .map { summary ->
                        val lastMessageAt = Instant.ofEpochMilli(summary.lastMessageAtEpochMillis).atZone(zone)
                        HistoryItem(summary.id, summary.title, lastMessageAt.toLocalDateTime(), summary.isOpen)
                    }.groupBy { periodOf(it.lastMessageAt.toLocalDate(), today) }
                    .map { (period, items) -> HistoryGroup(period, items) }
            return ConversationHistoryUiState(
                isOpen = panel.isOpen,
                isLoading = conversations == null,
                groups = groups,
                hasHistory = !conversations.isNullOrEmpty(),
                isConfirmingClear = panel.isConfirmingClear,
            )
        }

        private fun periodOf(
            date: LocalDate,
            today: LocalDate,
        ): HistoryPeriod =
            when {
                !date.isBefore(today) -> HistoryPeriod.Today
                date == today.minusDays(1) -> HistoryPeriod.Yesterday
                !date.isBefore(today.minusDays(LAST_WEEK_DAYS)) -> HistoryPeriod.LastSevenDays
                else -> HistoryPeriod.Older
            }
    }

private val AttachmentRejection.messageRes: Int
    get() =
        when (this) {
            AttachmentRejection.UnsupportedType -> R.string.assistant_attachment_unsupported
            AttachmentRejection.TooLarge -> R.string.assistant_attachment_too_large
            AttachmentRejection.TooMany -> R.string.assistant_attachment_too_many
        }
