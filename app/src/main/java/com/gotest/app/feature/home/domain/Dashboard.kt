package com.gotest.app.feature.home.domain

import com.gotest.app.feature.practice.domain.PracticePackage
import com.gotest.app.feature.tryout.domain.TryoutPackage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Beranda (T-026 A2c-1 = a): `GET /dashboard`, the same data as the web page `UserDashboard`
 * (`StudentDashboardData`). The "Paket Untukmu" packages are the catalog's rows, so their start sheets are the
 * catalog's (web PracticeStartModal and TryoutStartModal).
 */
data class DashboardPage(
    val schoolSpaces: List<SchoolSpace> = emptyList(),
    val mitraTasks: List<MitraTaskCard> = emptyList(),
    val practicePackages: List<PracticePackage> = emptyList(),
    val tryoutPackages: List<TryoutPackage> = emptyList(),
    val recentAttempts: List<DashboardAttempt> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val continueAttempt: ContinueAttempt? = null,
    val leaderboard: WeeklyLeaderboard = WeeklyLeaderboard(),
    val banner: DashboardBanner? = null,
)

/** A Mitra the account belongs to, with its next Try Out (DEC-088). */
@Serializable
data class SchoolSpace(
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    /** `siswa`, or `pengelola`/`pengajar`, whose portal is on gotest.id only (A2b-2). */
    @SerialName("role") val role: String,
    @SerialName("nextSession") val nextSession: NextSession? = null,
) {
    val isStudent: Boolean get() = role == STUDENT

    companion object {
        const val STUDENT = "siswa"
    }
}

@Serializable
data class NextSession(
    @SerialName("title") val title: String,
    /** `scheduled`, `running`, or `closed`. */
    @SerialName("state") val state: String,
    @SerialName("startsAt") val startsAt: String,
) {
    val isRunning: Boolean get() = state == "running"
}

/** "Tugas hari ini" of one Mitra (K-RB12 = A): only Mitra with tasks to do today or late. */
@Serializable
data class MitraTaskCard(
    @SerialName("organizationCode") val organizationCode: String,
    @SerialName("organizationName") val organizationName: String,
    @SerialName("todayCount") val todayCount: Int = 0,
    @SerialName("lateCount") val lateCount: Int = 0,
)

/** One of "Aktivitas Terkini". */
@Serializable
data class DashboardAttempt(
    @SerialName("id") val id: Long,
    @SerialName("packageName") val packageName: String,
    @SerialName("categoryName") val categoryName: String? = null,
    /** `practice` or `tryout`. */
    @SerialName("mode") val mode: String = MODE_PRACTICE,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("scoreLabel") val scoreLabel: String? = null,
    @SerialName("submittedAt") val submittedAt: String? = null,
) {
    val isTryout: Boolean get() = mode == MODE_TRYOUT
}

@Serializable
data class DashboardStats(
    @SerialName("questionsDone") val questionsDone: Int = 0,
    @SerialName("accuracy") val accuracy: Int = 0,
    @SerialName("packagesDone") val packagesDone: Int = 0,
)

/** The attempt behind "Lanjutkan Latihan" or "Lanjutkan Try Out". */
@Serializable
data class ContinueAttempt(
    @SerialName("id") val id: Long,
    @SerialName("mode") val mode: String = MODE_PRACTICE,
    @SerialName("packageName") val packageName: String,
    @SerialName("categoryName") val categoryName: String? = null,
    @SerialName("questionCount") val questionCount: Int = 0,
    @SerialName("answeredCount") val answeredCount: Int = 0,
    @SerialName("durationMinutes") val durationMinutes: Int? = null,
) {
    val isTryout: Boolean get() = mode == MODE_TRYOUT
}

/** The week's Try Out ranking (Monday 00:00 WIB): the top three and the viewer. */
@Serializable
data class WeeklyLeaderboard(
    @SerialName("top") val top: List<WeeklyRow> = emptyList(),
    @SerialName("current") val current: WeeklyRow? = null,
)

@Serializable
data class WeeklyRow(
    @SerialName("rank") val rank: Int,
    @SerialName("userId") val userId: Long,
    @SerialName("name") val name: String,
    @SerialName("points") val points: Int = 0,
)

/** The banner the content team sets in the dashboard; [href] is a web address. */
@Serializable
data class DashboardBanner(
    @SerialName("title") val title: String = "",
    @SerialName("altText") val altText: String = "",
    @SerialName("href") val href: String? = null,
    @SerialName("imageUrl") val imageUrl: String,
)

const val MODE_PRACTICE = "practice"
const val MODE_TRYOUT = "tryout"

interface DashboardRepository {
    suspend fun dashboard(): Result<DashboardPage>
}
