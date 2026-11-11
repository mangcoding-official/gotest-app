package com.gotest.app.feature.assistant.data

import com.gotest.app.core.data.fake.FakeAccountStore
import com.gotest.app.core.data.fake.PackageIds
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AssistantAttachment
import com.gotest.app.feature.assistant.domain.AssistantConversation
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantRepository
import com.gotest.app.feature.assistant.domain.AssistantSender
import com.gotest.app.feature.assistant.domain.ConversationSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private const val LISTEN_START_DELAY_MS = 600L
private const val WORD_INTERVAL_MS = 350L

/** What the simulated speech recogniser "hears" (the design's sample transcript). */
private const val SAMPLE_TRANSCRIPT = "Aku mau latihan matematika buat persiapan UTBK"

/** A conversation as the fake keeps it. Only conversations with messages are stored. */
private data class StoredConversation(
    val id: String,
    val title: String,
    val messages: List<AssistantMessage>,
    val pendingReplies: Int = 0,
) {
    val lastMessageAt: Long get() = messages.last().sentAtEpochMillis
}

/**
 * In-memory assistant. Answers after [simulateLatency] with canned replies picked by keyword, so
 * the prompts from the design (rekomendasi, tryout, lanjutkan, topik, peringkat) all lead
 * somewhere. Voice input replays [SAMPLE_TRANSCRIPT] word by word; no microphone is used.
 * The history starts with the five conversations of "Riwayat Percakapan" (see
 * [AssistantSeedConversations]); the app opens on a new, empty conversation.
 */
@Singleton
class FakeAssistantRepository
    @Inject
    constructor(
        private val accountStore: FakeAccountStore,
        private val studyPackageRepository: StudyPackageRepository,
    ) : AssistantRepository {
        private val conversations = MutableStateFlow<List<StoredConversation>>(emptyList())
        private val nextMessageId = AtomicLong()
        private val nextConversationId = AtomicLong()
        private val currentId = MutableStateFlow(newConversationId())
        private val seedLock = Mutex()
        private var isSeeded = false

        override fun observeCurrentConversation(): Flow<AssistantConversation> =
            flow {
                ensureSeeded()
                emitAll(
                    combine(conversations, currentId) { stored, id ->
                        val conversation = stored.firstOrNull { it.id == id }
                        AssistantConversation(
                            id = id,
                            messages = conversation?.messages.orEmpty(),
                            isAwaitingReply = (conversation?.pendingReplies ?: 0) > 0,
                        )
                    },
                )
            }.withFakeLatency()

        override fun observeHistory(): Flow<List<ConversationSummary>> =
            flow {
                ensureSeeded()
                emitAll(
                    combine(conversations, currentId) { stored, id ->
                        stored.sortedByDescending { it.lastMessageAt }.map { conversation ->
                            ConversationSummary(
                                id = conversation.id,
                                title = conversation.title,
                                lastMessageAtEpochMillis = conversation.lastMessageAt,
                                isOpen = conversation.id == id,
                            )
                        }
                    },
                )
            }.withFakeLatency()

        override suspend fun sendMessage(
            text: String,
            voiceDurationSeconds: Int?,
            attachments: List<AssistantAttachment>,
        ): Result<Unit> {
            val prompt = text.trim()
            if (prompt.isEmpty() && attachments.isEmpty()) {
                return Result.failure(IllegalArgumentException("empty message"))
            }
            ensureSeeded()
            val conversationId = currentId.value
            val question = userMessage(prompt, voiceDurationSeconds, attachments)
            conversations.update { stored ->
                val existing = stored.firstOrNull { it.id == conversationId }
                if (existing == null) {
                    val greeting = assistantMessage(greetingFor(accountStore.profile.value.firstName))
                    val title = prompt.ifEmpty { attachments.first().name }
                    stored + StoredConversation(conversationId, title, listOf(greeting, question), pendingReplies = 1)
                } else {
                    stored.updateConversation(conversationId) {
                        it.copy(messages = it.messages + question, pendingReplies = it.pendingReplies + 1)
                    }
                }
            }
            simulateLatency()
            val reply = if (attachments.isEmpty()) replyTo(prompt.lowercase()) else attachmentReply(attachments)
            // A conversation cleared in the meantime simply doesn't get the answer.
            conversations.update { stored ->
                stored.updateConversation(conversationId) {
                    it.copy(messages = it.messages + reply, pendingReplies = it.pendingReplies - 1)
                }
            }
            return Result.success(Unit)
        }

        override fun startNewConversation() {
            // The current conversation is already new while it has no messages.
            if (conversations.value.any { it.id == currentId.value }) currentId.value = newConversationId()
        }

        override fun openConversation(conversationId: String) {
            currentId.value = conversationId
        }

        override suspend fun clearHistory(): Result<Unit> {
            ensureSeeded()
            simulateLatency()
            conversations.value = emptyList()
            currentId.value = newConversationId()
            return Result.success(Unit)
        }

        override fun listen(): Flow<String> =
            flow {
                delay(LISTEN_START_DELAY_MS)
                val words = SAMPLE_TRANSCRIPT.split(' ')
                for (count in 1..words.size) {
                    emit(words.take(count).joinToString(" "))
                    delay(WORD_INTERVAL_MS)
                }
            }

        /** Adds the design's history once; the recommendations need package details first. */
        private suspend fun ensureSeeded() {
            seedLock.withLock {
                if (isSeeded) return
                val seeds =
                    AssistantSeedConversations.create(
                        firstName = accountStore.profile.value.firstName,
                        nowEpochMillis = System.currentTimeMillis(),
                        aljabarPackages = packages(PackageIds.LATIHAN_ALJABAR, PackageIds.LATIHAN_ALJABAR_LINEAR),
                        logaritmaPackages = packages(PackageIds.LATIHAN_LOGARITMA),
                        newMessageId = ::newMessageId,
                    )
                conversations.update { stored ->
                    stored + seeds.map { StoredConversation(it.id, it.title, it.messages) }
                }
                isSeeded = true
            }
        }

        private suspend fun replyTo(prompt: String): AssistantMessage =
            when {
                prompt.containsAny("lanjut", "belum selesai") -> continueReply()
                prompt.containsAny("topik", "perbaiki", "lemah") -> topicsReply()
                prompt.containsAny("peringkat", "ranking") -> rankReply()
                prompt.containsAny("tryout", "try out", "simulasi") -> tryoutReply()
                prompt.containsAny("rekomendasi", "latihan", "bingung", "belajar") -> recommendationReply()
                else -> assistantMessage(FALLBACK_REPLY)
            }

        private suspend fun recommendationReply() =
            assistantMessage(
                text = AssistantSeedConversations.RECOMMENDATION_REPLY,
                packages = packages(PackageIds.LATIHAN_ALJABAR, PackageIds.LATIHAN_ALJABAR_LINEAR),
            )

        private fun tryoutReply() =
            assistantMessage(
                text = AssistantSeedConversations.TRYOUT_REPLY,
                actions = listOf(AssistantAction.StartTryout(PackageIds.TRYOUT_UTBK_1), AssistantAction.BrowseTryouts),
            )

        private suspend fun continueReply(): AssistantMessage {
            val inProgress = studyPackageRepository.observeInProgress().first()
            return if (inProgress == null) {
                assistantMessage("Semua latihanmu sudah selesai. Mau aku rekomendasikan latihan baru?")
            } else {
                assistantMessage(
                    text = "Kamu masih punya latihan yang belum selesai. Yuk lanjutkan dari soal terakhirmu:",
                    packages = listOf(inProgress),
                )
            }
        }

        private suspend fun topicsReply() =
            assistantMessage(
                text =
                    "Topik yang paling perlu kamu perkuat: Aljabar (akurasi 25%) dan Logaritma (akurasi 70%). " +
                        "Mulai dari latihan ini ya:",
                packages = packages(PackageIds.LATIHAN_ALJABAR, PackageIds.LATIHAN_LOGARITMA),
            )

        private fun rankReply() =
            assistantMessage(
                text =
                    "Peringkat terbaikmu #12 dari 12.512 peserta di Simulasi UTBK-SNBT September Session 1. " +
                        "Sedikit lagi masuk 10 besar!",
                actions = listOf(AssistantAction.OpenLeaderboard),
            )

        /** Acknowledges the files; reading them needs the real assistant API. */
        private fun attachmentReply(attachments: List<AssistantAttachment>) =
            assistantMessage(
                "Aku sudah terima ${attachments.size} lampiran: ${attachments.joinToString { it.name }}. " +
                    "Bagian mana yang mau kita bahas dulu? Tulis nomor soal atau bagian yang bikin bingung ya.",
            )

        private suspend fun packages(vararg ids: String): List<StudyPackage> =
            coroutineScope {
                ids
                    .map { id -> async { studyPackageRepository.observePackageDetail(id).first().summary } }
                    .awaitAll()
            }

        private fun userMessage(
            text: String,
            voiceDurationSeconds: Int?,
            attachments: List<AssistantAttachment>,
        ) = AssistantMessage(
            id = newMessageId(),
            sender = AssistantSender.User,
            text = text,
            sentAtEpochMillis = System.currentTimeMillis(),
            voiceDurationSeconds = voiceDurationSeconds,
            attachments = attachments,
        )

        private fun assistantMessage(
            text: String,
            packages: List<StudyPackage> = emptyList(),
            actions: List<AssistantAction> = emptyList(),
        ) = AssistantMessage(
            id = newMessageId(),
            sender = AssistantSender.Assistant,
            text = text,
            sentAtEpochMillis = System.currentTimeMillis(),
            packages = packages,
            actions = actions,
        )

        private fun newMessageId(): String = "msg_${nextMessageId.incrementAndGet()}"

        private fun newConversationId(): String = "conv_${nextConversationId.incrementAndGet()}"

        private fun greetingFor(firstName: String) = AssistantSeedConversations.greetingFor(firstName)

        private fun String.containsAny(vararg keywords: String): Boolean = keywords.any { contains(it) }

        private fun List<StoredConversation>.updateConversation(
            id: String,
            transform: (StoredConversation) -> StoredConversation,
        ): List<StoredConversation> = map { if (it.id == id) transform(it) else it }

        private companion object {
            const val FALLBACK_REPLY =
                "Maaf, aku belum paham. Coba tanya soal rekomendasi latihan, tryout, atau topik yang perlu kamu " +
                    "perbaiki ya."
        }
    }
