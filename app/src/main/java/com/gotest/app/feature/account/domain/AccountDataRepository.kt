package com.gotest.app.feature.account.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Profile fields only Data Pribadi shows; name and email come from the shared profile. */
data class PersonalDetails(
    val phoneNumber: String,
    val birthDate: LocalDate,
    val school: String,
    val city: String,
)

/** The "Pilih data" options on Unduh Data Akun. */
enum class AccountDataCategory {
    /** "Profil & akun": name, email, main level, settings. */
    ProfileAndAccount,

    /** "Riwayat latihan & tryout": attempts, answers and scores. */
    PracticeHistory,

    /** "Percakapan Asisten": every chat with the assistant. */
    AssistantChats,

    /** "Transaksi & langganan": invoices and payment status. */
    Transactions,
}

/** The student's stored personal data (Data Pribadi) and copies of it (Unduh Data Akun). */
interface AccountDataRepository {
    fun observePersonalDetails(): Flow<PersonalDetails>

    /** Asks for a ZIP of [categories]; the download link is emailed to the account's address. */
    suspend fun requestDataCopy(categories: Set<AccountDataCategory>): Result<Unit>
}
