package com.gotest.app.feature.assistant.data

import com.gotest.app.core.data.fake.FakeAccountStore
import com.gotest.app.core.data.fake.PackageIds
import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.feature.assistant.domain.AssistantAction
import com.gotest.app.feature.assistant.domain.AssistantMessage
import com.gotest.app.feature.assistant.domain.AssistantRepository
import com.gotest.app.feature.assistant.domain.AssistantSender
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private const val LISTEN_START_DELAY_MS = 600L
private const val WORD_INTERVAL_MS = 350L

/** What the simulated speech recogniser "hears" (the design's sample transcript). */
private const val SAMPLE_TRANSCRIPT = "Aku mau latihan matematika buat persiapan UTBK"

/**
 * In-memory assistant. Answers after [simulateLatency] with canned replies picked by keyword, so
 * the prompts from the design (rekomendasi, tryout, lanjutkan, topik, peringkat) all lead
 * somewhere. Voice input replays [SAMPLE_TRANSCRIPT] word by word; no microphone is used.
 */
@Singleton
class FakeAssistantRepository
    @Inject
    constructor(
        private val accountStore: FakeAccountStore,
        private val studyPackageRepository: StudyPackageRepository,
    ) : AssistantRepository {
        private val messages = MutableStateFlow<List<AssistantMessage>>(emptyList())
        private val nextId = AtomicLong()

        override fun observeMessages(): Flow<List<AssistantMessage>> = messages.withFakeLatency()

        override suspend fun sendMessage(
            text: String,
            voiceDurationSeconds: Int?,
        ): Result<Unit> {
            val prompt = text.trim()
            if (prompt.isEmpty()) return Result.failure(IllegalArgumentException("empty message"))
            val firstName = accountStore.profile.value.firstName
            messages.update { current ->
                val greeting = if (current.isEmpty()) listOf(assistantMessage(greetingFor(firstName))) else emptyList()
                current + greeting + userMessage(prompt, voiceDurationSeconds)
            }
            simulateLatency()
            val reply = replyTo(prompt.lowercase())
            messages.update { it + reply }
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
                text = "Dari hasil terakhirmu, akurasi topik Aljabar masih 25%. Coba mulai dari latihan ini ya:",
                packages = packages(PackageIds.LATIHAN_ALJABAR, PackageIds.LATIHAN_ALJABAR_LINEAR),
            )

        private fun tryoutReply() =
            assistantMessage(
                text = "Ada! Simulasi UTBK-SNBT Session 1 tersedia sampai 3 hari lagi. Mau langsung mulai?",
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

        private suspend fun packages(vararg ids: String): List<StudyPackage> =
            coroutineScope {
                ids
                    .map { id -> async { studyPackageRepository.observePackageDetail(id).first().summary } }
                    .awaitAll()
            }

        private fun userMessage(
            text: String,
            voiceDurationSeconds: Int?,
        ) = AssistantMessage(
            id = newId(),
            sender = AssistantSender.User,
            text = text,
            sentAtEpochMillis = System.currentTimeMillis(),
            voiceDurationSeconds = voiceDurationSeconds,
        )

        private fun assistantMessage(
            text: String,
            packages: List<StudyPackage> = emptyList(),
            actions: List<AssistantAction> = emptyList(),
        ) = AssistantMessage(
            id = newId(),
            sender = AssistantSender.Assistant,
            text = text,
            sentAtEpochMillis = System.currentTimeMillis(),
            packages = packages,
            actions = actions,
        )

        private fun newId(): String = "msg_${nextId.incrementAndGet()}"

        private fun greetingFor(firstName: String) = "Halo $firstName! Aku asisten belajarmu. Mau latihan apa hari ini?"

        private fun String.containsAny(vararg keywords: String): Boolean = keywords.any { contains(it) }

        private companion object {
            const val FALLBACK_REPLY =
                "Maaf, aku belum paham. Coba tanya soal rekomendasi latihan, tryout, atau topik yang perlu kamu " +
                    "perbaiki ya."
        }
    }
