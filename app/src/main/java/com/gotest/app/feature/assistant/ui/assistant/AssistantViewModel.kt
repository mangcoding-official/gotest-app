package com.gotest.app.feature.assistant.ui.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotest.app.R
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantRepository
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

@HiltViewModel
class AssistantViewModel
    @Inject
    constructor(
        private val assistantRepository: AssistantRepository,
        userRepository: UserRepository,
    ) : ViewModel() {
        /** Text field content; Compose state so typing stays in sync with the field. */
        var input by mutableStateOf("")
            private set

        private val refresh = MutableStateFlow(0)
        private val pendingReplies = MutableStateFlow(0)
        private val voice = MutableStateFlow<VoiceInputState?>(null)
        private var voiceJob: Job? = null

        private val _effects = Channel<AssistantEffect>(Channel.BUFFERED)
        val effects: Flow<AssistantEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<AssistantUiState> =
            refresh
                .flatMapLatest {
                    combine(
                        userRepository.observeProfile().map { it.firstName }.distinctUntilChanged(),
                        assistantRepository.observeMessages(),
                        pendingReplies,
                        voice,
                        ::toUiState,
                    ).onStart { emit(AssistantUiState.Loading) }
                        .catch { emit(AssistantUiState.Error) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AssistantUiState.Loading)

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
            }
        }

        private fun send(
            text: String,
            voiceDurationSeconds: Int? = null,
        ) {
            val message = text.trim()
            if (message.isEmpty()) return
            pendingReplies.update { it + 1 }
            viewModelScope.launch {
                assistantRepository
                    .sendMessage(message, voiceDurationSeconds)
                    .onFailure { _effects.send(AssistantEffect.ShowMessage(R.string.assistant_send_error)) }
                pendingReplies.update { it - 1 }
            }
        }

        private fun startVoice() {
            if (voiceJob?.isActive == true) return
            voice.value = VoiceInputState()
            voiceJob =
                viewModelScope.launch {
                    launch {
                        while (isActive) {
                            delay(ONE_SECOND_MS)
                            voice.update { it?.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                        }
                    }
                    assistantRepository.listen().collect { transcript ->
                        voice.update { it?.copy(transcript = transcript) }
                    }
                }
        }

        private fun finishVoice() {
            val recording = voice.value ?: return
            stopVoice()
            send(recording.transcript, voiceDurationSeconds = recording.elapsedSeconds.coerceAtLeast(1))
        }

        private fun stopVoice() {
            voiceJob?.cancel()
            voiceJob = null
            voice.value = null
        }

        private fun toUiState(
            firstName: String,
            messages: List<AssistantMessage>,
            pending: Int,
            voiceInput: VoiceInputState?,
        ): AssistantUiState =
            if (messages.isEmpty()) {
                AssistantUiState.Empty(firstName = firstName, voice = voiceInput)
            } else {
                AssistantUiState.Success(items = chatItems(messages, isReplying = pending > 0), voice = voiceInput)
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
    }
