package com.gotest.app.core.model

import java.time.LocalDate

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    /** Coil model: a URL, or an `android.resource://` URI for the dummy avatars. */
    val avatarUrl: String?,
    val stats: UserStats,
    val subscription: Subscription,
) {
    /** "Halo, Gilang" uses the first name only. */
    val firstName: String get() = fullName.substringBefore(' ')

    /** Short form used in leaderboards, e.g. "Gilang P.". */
    val shortName: String
        get() {
            val parts = fullName.split(' ')
            return if (parts.size < 2) fullName else "${parts.first()} ${parts[1].first()}."
        }
}

data class UserStats(
    val questionsAnswered: Int,
    val accuracyPercent: Int,
    val packagesCompleted: Int,
    /** Week-over-week change shown next to each number on Beranda, in percent. */
    val questionsTrendPercent: Int,
    val accuracyTrendPercent: Int,
    val packagesTrendPercent: Int,
)

enum class PlanTier { FreeTrial, Premium }

data class Subscription(
    val tier: PlanTier,
    val planName: String,
    val activatedOn: LocalDate?,
    val activeUntil: LocalDate,
    val paymentMethod: String?,
) {
    val isPremium: Boolean get() = tier == PlanTier.Premium
}
