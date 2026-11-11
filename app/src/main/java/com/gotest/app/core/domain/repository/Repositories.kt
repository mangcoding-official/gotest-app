package com.gotest.app.core.domain.repository

import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.ExamSession
import com.gotest.app.core.model.GlobalLeaderboard
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.PackageLeaderboard
import com.gotest.app.core.model.PackageRanking
import com.gotest.app.core.model.PackageType
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.QuestionAnswer
import com.gotest.app.core.model.QuestionReview
import com.gotest.app.core.model.SeasonalTryout
import com.gotest.app.core.model.StudyCategory
import com.gotest.app.core.model.StudyPackage
import com.gotest.app.core.model.StudyPackageDetail
import com.gotest.app.core.model.Subject
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.Topic
import com.gotest.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Shared repositories. Today every one is bound to a Fake implementation in core/data;
// swapping the Hilt binding to a remote implementation is all the API work needs.

interface AuthRepository {
    val isSignedIn: StateFlow<Boolean>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<Unit>

    suspend fun signInWithGoogle(): Result<Unit>

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): Result<Unit>

    suspend fun requestPasswordReset(email: String): Result<Unit>

    suspend fun signOut()
}

interface UserRepository {
    fun observeProfile(): Flow<UserProfile>

    suspend fun updateProfile(
        fullName: String,
        email: String,
    ): Result<Unit>

    suspend fun updateAvatar(avatarUrl: String): Result<Unit>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}

interface StudyPackageRepository {
    /** "Paket Untukmu" on Beranda, per tab. */
    fun observeRecommended(type: PackageType): Flow<List<StudyPackage>>

    /** The package the student left unfinished, if any ("Lanjutkan" card). */
    fun observeInProgress(): Flow<StudyPackage?>

    fun observeCategories(type: PackageType): Flow<List<StudyCategory>>

    fun observeSubjects(categoryId: String): Flow<List<Subject>>

    fun observeCategory(categoryId: String): Flow<StudyCategory>

    fun observeTopics(subjectId: String): Flow<List<Topic>>

    fun observeSubject(subjectId: String): Flow<Subject>

    /** Flat list for "Paket Belajar"; [levelLabel] null means every level. */
    fun observePackages(
        type: PackageType,
        levelLabel: String?,
    ): Flow<List<StudyPackage>>

    fun observeLevels(): Flow<List<String>>

    fun observePackageDetail(packageId: String): Flow<StudyPackageDetail>

    fun observeSeasonalTryout(): Flow<SeasonalTryout?>
}

interface ExamRepository {
    /** Resumes the open attempt for [packageId] or starts a new one. */
    suspend fun startOrResume(packageId: String): ExamSession

    suspend fun saveAnswer(
        attemptId: String,
        questionId: String,
        answer: QuestionAnswer,
    )

    suspend fun setMarked(
        attemptId: String,
        questionId: String,
        marked: Boolean,
    )

    /** Grades the attempt and returns its id for the result screen. */
    suspend fun submit(attemptId: String): String

    fun observeResult(attemptId: String): Flow<AttemptResult>

    fun observeReview(attemptId: String): Flow<List<QuestionReview>>

    /** Finished attempts, newest first (Riwayat, Aktivitas Terkini). */
    fun observeHistory(): Flow<List<AttemptSummary>>
}

interface LeaderboardRepository {
    fun observeGlobal(): Flow<GlobalLeaderboard>

    fun observePackageRankings(): Flow<List<PackageRanking>>

    fun observePackageLeaderboard(packageId: String): Flow<PackageLeaderboard>
}

interface SubscriptionRepository {
    fun observeSubscription(): Flow<Subscription>

    fun observePlans(): Flow<List<Plan>>

    fun observePaymentHistory(): Flow<List<PaymentRecord>>

    suspend fun createOrder(
        planId: String,
        method: PaymentMethodType,
    ): Order

    fun observeOrder(orderId: String): Flow<Order>

    /** "Saya Sudah Membayar": the fake marks the order paid and activates Premium. */
    suspend fun confirmPayment(orderId: String): Result<Unit>
}
