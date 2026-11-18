package com.gotest.app.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation keys for every destination found in the design audit (Phase 1).

// Auth
@Serializable data object OnboardingDestination

@Serializable data object LoginDestination

@Serializable data object RegisterDestination

@Serializable data object ForgotPasswordDestination

/** "Buat kata sandi baru" from the reset email link (T-018). */
@Serializable data class ResetPasswordDestination(
    val token: String,
    val email: String? = null,
)

/** "Satu langkah lagi" for a new number after its WhatsApp code (T-018). */
@Serializable data class PhoneRegisterDestination(
    val phoneDisplay: String,
)

/** Masuk Anak: family code, name, PIN (T-018). */
@Serializable data object ChildLoginDestination

/** "Menunggu persetujuan orang tua" for an under-age account (Aturan 2.15). */
@Serializable data object GuardianConsentDestination

// Top-level tabs

@Serializable data object HomeDestination

@Serializable data object StudyPackageDestination

@Serializable data object HistoryDestination

@Serializable data object LeaderboardDestination

@Serializable data object ProfileDestination

// Home
@Serializable data object SearchDestination

@Serializable data object NotificationDestination

/** A promo opened from Beranda's banner (e.g. "Semangat Merdeka"). */
@Serializable data class PromoDestination(
    val promoId: String,
)

// Study packages ("Paket")
@Serializable data class PackageListDestination(
    val categoryId: String? = null,
)

@Serializable data class SubjectListDestination(
    val categoryId: String,
)

@Serializable data class TopicListDestination(
    val subjectId: String,
)

@Serializable data class PracticeDetailDestination(
    val packageId: String,
)

@Serializable data class TryoutDetailDestination(
    val packageId: String,
)

@Serializable data object SavedPackagesDestination

// Exam ("Ujian")
@Serializable data class ExamDestination(
    val packageId: String,
)

@Serializable data class ExamResultDestination(
    val attemptId: String,
)

@Serializable data class DiscussionDestination(
    val attemptId: String,
    val questionNumber: Int = 1,
)

@Serializable data class DiscussionSummaryDestination(
    val attemptId: String,
)

// History
@Serializable data class HistoryDetailDestination(
    val attemptId: String,
)

// Leaderboard
@Serializable data class PackageLeaderboardDestination(
    val packageId: String,
)

@Serializable data class PackageLeaderboardSearchDestination(
    val packageId: String,
)

// Account
@Serializable data object EditProfileDestination

@Serializable data object ChangePasswordDestination

@Serializable data object DeleteAccountDestination

/** "Nomor WhatsApp" on Profil (DEC-123). */
@Serializable data object AccountPhoneDestination

/** "Sesi Aktif" on Profil (DEC-102). */
@Serializable data object ActiveSessionsDestination

@Serializable data object SettingsDestination

@Serializable data object MainLevelDestination

@Serializable data object PersonalDataDestination

@Serializable data object DownloadAccountDataDestination

@Serializable data object SubscriptionDestination

@Serializable data object HelpDestination

@Serializable data class HelpTopicDestination(
    val topicId: String,
)

// Upgrade
@Serializable data object UpgradePlanDestination

/** [promoId] applies a promo's discount (e.g. from [PromoDestination]). */
@Serializable data class CheckoutDestination(
    val planId: String,
    val promoId: String? = null,
)

@Serializable data class PaymentPendingDestination(
    val orderId: String,
)

@Serializable data class PaymentSuccessDestination(
    val orderId: String,
)

// AI assistant
@Serializable data object AssistantDestination

// Main Bareng (multiplayer)

/**
 * [openJoinSheet] opens "Gabung dengan kode" straight away (Beranda's "Gabung dengan Kode");
 * [joinCode] fills it in, from a `gotest.id/main-bareng?kode=…` link (T-018).
 */
@Serializable data class MultiplayerDestination(
    val openJoinSheet: Boolean = false,
    val joinCode: String? = null,
)

/** [copyFromGameCode] prefills the form with that game's settings ("Buat Ulang dengan Pengaturan Sama"). */
@Serializable data class CreateGameDestination(
    val copyFromGameCode: String? = null,
)

@Serializable data class WaitingRoomDestination(
    val gameCode: String,
)

@Serializable data class GamePlayDestination(
    val gameCode: String,
)

@Serializable data class GameWaitingDestination(
    val gameCode: String,
)

@Serializable data class GameResultDestination(
    val gameCode: String,
)

@Serializable data object MultiplayerHistoryDestination

@Serializable data class GameDetailDestination(
    val gameCode: String,
)

// Ruang Mitra and Ujian for students (T-026 A2b, API module G).
@Serializable data class RuangDestination(
    val code: String,
)

@Serializable data class UjianListDestination(
    val code: String,
)

@Serializable data class UjianDetailDestination(
    val code: String,
    val scheduleId: Long,
)

@Serializable data class UjianWorkDestination(
    val code: String,
    val attemptId: Long,
)

@Serializable data class UjianResultDestination(
    val code: String,
    val attemptId: Long,
)

/** "Gabung ke kelas": type a class or event code. */
@Serializable data object JoinWithCodeDestination

/** The page of one class or event code (web `/gabung/{code}`). */
@Serializable data class JoinCodeDestination(
    val code: String,
)

/** An invitation link (web `/undangan/{token}`). */
@Serializable data class InvitationDestination(
    val token: String,
)
