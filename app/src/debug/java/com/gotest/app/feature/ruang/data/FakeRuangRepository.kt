package com.gotest.app.feature.ruang.data

import com.gotest.app.core.data.fake.simulateLatency
import com.gotest.app.core.network.ApiError
import com.gotest.app.feature.ruang.domain.EmailProof
import com.gotest.app.feature.ruang.domain.InvitationPage
import com.gotest.app.feature.ruang.domain.JoinCodePage
import com.gotest.app.feature.ruang.domain.JoinedMitra
import com.gotest.app.feature.ruang.domain.MitraMembership
import com.gotest.app.feature.ruang.domain.MitraRef
import com.gotest.app.feature.ruang.domain.RuangPage
import com.gotest.app.feature.ruang.domain.RuangRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug data for API module G (debug builds only, T-026 criterion 8). Gilang is a student of
 * SMP 1 Bantargedung and was a student of Bimbel Cerdas. Codes to try in "Gabung ke kelas":
 * `KELAS8A` (class code, ready), `PENUH01` (full), `EVENT01` (event code, ready); anything else is not
 * found. Invitation tokens: `undangan-siap`, `undangan-kode` (needs the email code `123456`),
 * `undangan-terpakai`. Texts are the server's (JoinCodePages, InvitationPages). Tugas hari ini and the
 * plan progress come from [FakeStudyPlanRepository].
 */
@Singleton
class FakeRuangRepository
    @Inject
    constructor(
        private val plans: FakeStudyPlanRepository,
    ) : RuangRepository {
        private val members =
            mutableListOf(
                MitraMembership(SMP, "SMP 1 Bantargedung", "sekolah", "siswa", former = false, hasExams = true),
                MitraMembership(BIMBEL, "Bimbel Cerdas", "bimbel", "siswa", former = true, hasExams = true),
            )
        private var invitationCodeSent = false
        private var wrongCodes = 0

        override suspend fun memberships(): Result<List<MitraMembership>> {
            simulateLatency()
            return Result.success(members.toList())
        }

        /** The default remembered sign-in: 90 days from now (API-K4). */
        override suspend fun signInEndsAt(): Result<String?> =
            Result.success(
                java.time.Instant
                    .now()
                    .plus(SIGN_IN_DAYS, java.time.temporal.ChronoUnit.DAYS)
                    .toString(),
            )

        override suspend fun ruang(code: String): Result<RuangPage> {
            simulateLatency()
            val member = members.firstOrNull { it.code == code } ?: return notFound()
            return Result.success(
                RuangPage(
                    organization = MitraRef(member.name, member.code),
                    former = member.former,
                    today = plans.today(code),
                    planProgress = plans.progressOf(code),
                ),
            )
        }

        override suspend fun lookupCode(typed: String): Result<String> {
            simulateLatency()
            val code = typed.filterNot(Char::isWhitespace).uppercase()
            return when {
                code.isEmpty() -> fieldError("code", "Tulis kode dari pengajar atau penyelenggaramu.")
                code in CODES -> Result.success(code)
                else -> fieldError("code", "Kode tidak ditemukan. Periksa lagi kodenya, lalu coba sekali lagi.")
            }
        }

        override suspend fun joinCode(code: String): Result<JoinCodePage> {
            simulateLatency()
            val joined = members.any { it.code == KELAS_MITRA && !it.former }
            return Result.success(
                when (code) {
                    "KELAS8A" -> {
                        JoinCodePage(
                            code = code,
                            state = if (joined) "joined" else "ready",
                            organizationName = "SMP 2 Cimahi",
                            kind = "class",
                            className = "8A",
                            message = "Kamu sudah bergabung di kelas 8A.".takeIf { joined },
                        )
                    }

                    "PENUH01" -> {
                        JoinCodePage(
                            code = code,
                            state = "full",
                            organizationName = "SMP 2 Cimahi",
                            kind = "class",
                            className = "8B",
                            message = "Kursi siswa SMP 2 Cimahi sudah penuh. Hubungi pengajarmu.",
                        )
                    }

                    "EVENT01" -> {
                        JoinCodePage(code, "ready", "Bimbel Juara", "Try Out Akbar Desember", "event")
                    }

                    else -> {
                        JoinCodePage(code, "invalid", kind = "class")
                    }
                },
            )
        }

        override suspend fun join(code: String): Result<JoinedMitra> {
            simulateLatency()
            val (mitraCode, name, className) =
                when (code) {
                    "KELAS8A" -> Triple(KELAS_MITRA, "SMP 2 Cimahi", "8A")
                    "EVENT01" -> Triple("BIMJUARA", "Bimbel Juara", null)
                    else -> return notFound()
                }
            if (members.none { it.code == mitraCode }) {
                members += MitraMembership(mitraCode, name, "sekolah", "siswa", hasExams = false)
            }
            return Result.success(JoinedMitra(mitraCode, "siswa", className))
        }

        override suspend fun invitation(token: String): Result<InvitationPage> {
            simulateLatency()
            return Result.success(
                when (token) {
                    "undangan-siap" -> {
                        InvitationPage(token, "ready", "SMA 3 Bandung", "gilangpermana@mail.com", role = "siswa")
                    }

                    "undangan-kode" -> {
                        InvitationPage(
                            token = token,
                            state = "ready",
                            organizationName = "Bimbel Juara",
                            email = "gilangpermana@mail.com",
                            role = "siswa",
                            emailProof =
                                EmailProof(
                                    sentTo = "gi*************@mail.com",
                                    codeSent = invitationCodeSent.takeIf { it },
                                ),
                        )
                    }

                    "undangan-terpakai" -> {
                        InvitationPage(token, "used", "SMA 3 Bandung")
                    }

                    else -> {
                        InvitationPage(token, "invalid")
                    }
                },
            )
        }

        override suspend fun sendInvitationCode(token: String): Result<String?> {
            simulateLatency()
            invitationCodeSent = true
            return Result.success("gi*************@mail.com".takeIf { token == "undangan-kode" })
        }

        override suspend fun acceptInvitation(
            token: String,
            code: String?,
        ): Result<JoinedMitra> {
            simulateLatency()
            if (token == "undangan-kode" && code != EMAIL_CODE) {
                return if (code.isNullOrBlank()) {
                    fieldError("code", "Tulis kode 6 angka dari email.")
                } else {
                    wrongCodes++
                    fieldError("code", "Kode salah. Sisa ${MAX_CODE_TRIES - wrongCodes} kali percobaan.")
                }
            }
            val (mitraCode, name) =
                if (token ==
                    "undangan-kode"
                ) {
                    "BIMJUARA" to "Bimbel Juara"
                } else {
                    "SMA3BDG" to "SMA 3 Bandung"
                }
            if (members.none { it.code == mitraCode }) members += MitraMembership(mitraCode, name, "sekolah", "siswa")
            return Result.success(JoinedMitra(mitraCode, "siswa", null))
        }

        private fun <T> notFound(): Result<T> =
            Result.failure(ApiError(NOT_FOUND, "Data tidak ditemukan.", "not_found"))

        private fun <T> fieldError(
            field: String,
            message: String,
        ): Result<T> = Result.failure(ApiError(UNPROCESSABLE, message, "validation", mapOf(field to listOf(message))))

        companion object {
            const val SMP = "SMP1BTG"
            const val BIMBEL = "BCERDAS"
            private const val KELAS_MITRA = "SMP2CMH"
            private const val EMAIL_CODE = "123456"
            private const val MAX_CODE_TRIES = 5
            private const val SIGN_IN_DAYS = 90L
            private const val UNPROCESSABLE = 422
            private const val NOT_FOUND = 404
            private val CODES = setOf("KELAS8A", "PENUH01", "EVENT01")
        }
    }
