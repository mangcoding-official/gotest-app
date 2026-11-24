package com.gotest.app.core.data.api

import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.EducationLevel
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.model.UserStats
import com.gotest.app.core.network.apiCall
import com.gotest.app.core.session.AccountChanges
import com.gotest.app.core.session.SessionStore
import com.gotest.app.feature.ruang.data.parseServerTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in account from `GET /akun`. It is read again after every saved account write (AccountChanges,
 * T-026 required 15) and after each sign-in, so name, email, photo, and access never stay old.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ApiUserRepository
    @Inject
    constructor(
        retrofit: Retrofit,
        private val json: Json,
        private val session: SessionStore,
        private val changes: AccountChanges,
    ) : UserRepository {
        private val api = retrofit.create(AuthApi::class.java)

        override fun observeProfile(): Flow<UserProfile> =
            combine(session.isSignedIn, changes.changes) { signedIn, _ -> signedIn }
                .filter { it }
                .flatMapLatest { flow { emit(apiCall(json) { api.account() }.getOrThrow().toProfile()) } }

        /** The photo is saved through Profil (`POST /profil/foto`); this only reads `/akun` again. */
        override suspend fun updateAvatar(avatarUrl: String): Result<Unit> {
            changes.markChanged()
            return Result.success(Unit)
        }

        /** Profil writes go through ProfilePagesRepository (module F); these old fakes' entry points are unused. */
        override suspend fun updateProfile(
            fullName: String,
            email: String,
        ): Result<Unit> = Result.failure(UnsupportedOperationException("profil"))

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = Result.failure(UnsupportedOperationException("profil"))

        override suspend fun deleteAccount(): Result<Unit> = Result.failure(UnsupportedOperationException("profil"))
    }

private val wib: ZoneId = ZoneId.of("Asia/Jakarta")

private fun String?.toWibDate(): LocalDate? =
    this?.let { runCatching { parseServerTime(it).atZone(wib).toLocalDate() }.getOrNull() }

/** `/akun` in the shape the screens already read. Beranda's numbers come from its own page, not from here. */
internal fun AccountPage.toProfile(): UserProfile =
    UserProfile(
        id = user.id.toString(),
        fullName = user.name,
        email = user.email.orEmpty(),
        avatarUrl = user.avatarUrl,
        stats = UserStats(0, 0, 0, 0, 0, 0),
        subscription =
            Subscription(
                tier = if (access.status == ACTIVE) PlanTier.Premium else PlanTier.FreeTrial,
                planName = access.plan ?: access.label.orEmpty(),
                activatedOn = access.startsAt.toWibDate(),
                activeUntil = access.endsAt.toWibDate() ?: LocalDate.now(wib),
                paymentMethod = null,
            ),
        phone = user.phone,
        // A child account is kept by its family Mitra (Aturan 11.30).
        managedBy = if (isChild) organizations.firstOrNull()?.name.orEmpty() else null,
        educationLevel = educationLevelOf(user.educationLevel),
    )

/** The level's code in the API (`app/Support/EducationLevel.php`). */
internal fun educationLevelOf(code: String?): EducationLevel? =
    when (code) {
        "sd" -> EducationLevel.Sd
        "smp" -> EducationLevel.Smp
        "sma" -> EducationLevel.SmaSmk
        "kuliah" -> EducationLevel.Kuliah
        "umum" -> EducationLevel.Graduated
        else -> null
    }

private const val ACTIVE = "active"
