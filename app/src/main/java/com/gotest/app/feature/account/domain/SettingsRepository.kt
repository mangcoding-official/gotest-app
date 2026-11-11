package com.gotest.app.feature.account.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/** Every on/off switch on the Pengaturan screen. */
enum class SettingSwitch {
    /** Notifikasi: "Aktivitas Pengerjaan". */
    ActivityResults,

    /** Notifikasi: "Peringkat & Pencapaian". */
    RankingUpdates,

    /** Notifikasi: "Paket Baru". */
    NewPackages,

    /** Notifikasi: "Langganan". */
    SubscriptionStatus,

    /** Preferensi Belajar: "Pengingat Belajar". */
    StudyReminder,

    /** Privasi & Data: "Email Promosi". */
    PromoEmails,
}

/** "Jenjang Utama": the exam the student prepares for; it drives Beranda's packages and tryouts. */
enum class MainLevel {
    UtbkSnbt,
    Cpns,
    SekolahKedinasan,
    SmaSlta,
    Smp,
}

data class AccountSettings(
    val switches: Map<SettingSwitch, Boolean>,
    /** Time of the daily study reminder. */
    val reminderTime: LocalTime,
    val mainLevel: MainLevel,
) {
    fun isOn(switch: SettingSwitch): Boolean = switches[switch] == true
}

/** Notification toggles, the study reminder and the main level of the signed-in student. */
interface SettingsRepository {
    fun observeSettings(): Flow<AccountSettings>

    suspend fun setSwitch(
        switch: SettingSwitch,
        enabled: Boolean,
    ): Result<Unit>

    suspend fun setMainLevel(level: MainLevel): Result<Unit>
}
