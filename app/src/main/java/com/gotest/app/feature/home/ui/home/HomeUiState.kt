package com.gotest.app.feature.home.ui.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gotest.app.R
import com.gotest.app.core.model.PackageType
import com.gotest.app.feature.home.domain.DashboardAttempt
import com.gotest.app.feature.home.domain.DashboardPage
import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.tryout.domain.TryoutPackage

/** Greeting row: avatar, first name and the bell's unread dot. */
@Immutable
data class HomeProfileSummary(
    val firstName: String,
    /** Coil model for the avatar. */
    val avatarUrl: String?,
    val hasUnreadNotifications: Boolean,
)

/** A "Paket Untukmu" card: the catalog's row of its mode, whose start sheet it opens (web SuggestedPackages). */
sealed interface HomePackage {
    val id: Long
    val name: String
    val description: String?
    val categoryName: String?
    val premium: Boolean
    val finishedCount: Int
    val lastScore: Double?
    val lastScoreLabel: String?
    val lastFinishedAt: String?

    /** Web `actionLabel`: Lanjutkan, Khusus Premium, Kerjakan Lagi, Lihat Detail, Latihan Lagi, Lihat Pembahasan. */
    @get:StringRes val actionLabel: Int

    data class Practice(
        val item: PracticePackage,
    ) : HomePackage {
        override val id get() = item.id
        override val name get() = item.name
        override val description get() = item.description
        override val categoryName get() = item.categoryName
        override val premium get() = item.premium
        override val finishedCount get() = item.finishedCount
        override val lastScore get() = item.lastScore
        override val lastScoreLabel get() = item.lastScoreLabel
        override val lastFinishedAt get() = item.lastFinishedAt
        override val actionLabel: Int
            get() =
                when {
                    item.continueAttemptId != null -> R.string.home_package_continue_practice

                    opensPriceList(
                        item.premiumLocked,
                        item.finishedCount,
                        item.allowRetry,
                    ) -> R.string.practice_premium_only

                    item.finishedCount > 0 && item.allowRetry -> R.string.home_package_retry_practice

                    item.finishedCount > 0 -> R.string.home_package_review

                    else -> R.string.home_package_start_practice
                }
    }

    data class Tryout(
        val item: TryoutPackage,
    ) : HomePackage {
        override val id get() = item.id
        override val name get() = item.name
        override val description get() = item.description
        override val categoryName get() = item.categoryName
        override val premium get() = item.premium
        override val finishedCount get() = item.finishedCount
        override val lastScore get() = item.lastScore
        override val lastScoreLabel: String? get() = null
        override val lastFinishedAt get() = item.lastFinishedAt
        override val actionLabel: Int
            get() =
                when {
                    item.continueAttemptId != null -> R.string.home_package_continue_tryout

                    opensPriceList(
                        item.premiumLocked,
                        item.finishedCount,
                        item.allowRetry,
                    ) -> R.string.practice_premium_only

                    item.finishedCount > 0 && item.allowRetry -> R.string.home_package_retry_tryout

                    item.finishedCount > 0 -> R.string.home_package_detail

                    else -> R.string.home_package_start_tryout
                }
    }
}

/** Web `opensPriceList`: a new start of a Premium package by a Gratis account (DEC-127 butir 8). */
private fun opensPriceList(
    locked: Boolean,
    finished: Int,
    allowRetry: Boolean,
): Boolean = locked && !(finished > 0 && !allowRetry)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Error : HomeUiState

    /** Beranda from `GET /dashboard` (T-026 A2c-1), with the mode chosen in each of its two tab rows. */
    @Immutable
    data class Ready(
        val profile: HomeProfileSummary,
        val page: DashboardPage,
        val packageType: PackageType = PackageType.Practice,
        val activityType: PackageType = PackageType.Practice,
    ) : HomeUiState {
        /** The first four of the chosen mode, as the web shows. */
        val packages: List<HomePackage>
            get() =
                if (packageType == PackageType.Tryout) {
                    page.tryoutPackages.map(HomePackage::Tryout)
                } else {
                    page.practicePackages.map(HomePackage::Practice)
                }.take(MAX_PACKAGES)

        /** The three latest attempts of the chosen mode. */
        val activity: List<DashboardAttempt>
            get() =
                page.recentAttempts.filter { it.isTryout == (activityType == PackageType.Tryout) }.take(
                    MAX_ACTIVITY,
                )
    }
}

private const val MAX_PACKAGES = 4
private const val MAX_ACTIVITY = 3

sealed interface HomeUiEvent {
    data class SelectRecommendedType(
        val type: PackageType,
    ) : HomeUiEvent

    data class SelectActivityType(
        val type: PackageType,
    ) : HomeUiEvent

    data object Retry : HomeUiEvent

    /** Back on Beranda: what changed meanwhile (a finished Latihan, a new task) shows. */
    data object Refresh : HomeUiEvent
}
