package com.gotest.app.core.data.hidden

import com.gotest.app.core.domain.repository.ExamRepository
import com.gotest.app.core.domain.repository.LeaderboardRepository
import com.gotest.app.core.domain.repository.StudyPackageRepository
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.model.AttemptResult
import com.gotest.app.core.model.AttemptSummary
import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.ExamSession
import com.gotest.app.core.model.GlobalLeaderboard
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.OrderPromo
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
import com.gotest.app.feature.account.domain.AccountDataCategory
import com.gotest.app.feature.account.domain.AccountDataRepository
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.MainLevel
import com.gotest.app.feature.account.domain.PersonalDetails
import com.gotest.app.feature.account.domain.SettingSwitch
import com.gotest.app.feature.account.domain.SettingsRepository
import com.gotest.app.feature.assistant.domain.AssistantAttachment
import com.gotest.app.feature.assistant.domain.AssistantConversation
import com.gotest.app.feature.assistant.domain.AssistantRepository
import com.gotest.app.feature.assistant.domain.ConversationSummary
import com.gotest.app.feature.home.domain.AppNotification
import com.gotest.app.feature.home.domain.NotificationRepository
import com.gotest.app.feature.studypackage.domain.SavedPackage
import com.gotest.app.feature.studypackage.domain.SavedPackageRepository
import com.gotest.app.feature.upgrade.domain.Promo
import com.gotest.app.feature.upgrade.domain.PromoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/*
 * Release builds of features the web does not have yet (AppFeatures, K-AN2, K-AN3) and of the T-018 screens taken
 * out of the graph (A2c-2): their screens are never opened, but Hilt still needs a binding for their view models.
 * Nothing here is made-up data: lists are empty, a single item fails with [HiddenFeature], and an action fails the
 * same way (T-026, guard `checkNoFakeData`). The debug build keeps the fakes in src/debug.
 */

/** A feature that is hidden in this build was asked for anyway. */
class HiddenFeature : IllegalStateException("Fitur ini belum tersedia di aplikasi.")

private fun <T> unavailable(): Flow<T> = flow { throw HiddenFeature() }

private fun <T> refused(): Result<T> = Result.failure(HiddenFeature())

/** The T-018 Latihan catalog; Paket Latihan and Paket Try Out follow the web's catalog (A2c). */
class HiddenStudyPackageRepository
    @Inject
    constructor() : StudyPackageRepository {
        override fun observeRecommended(type: PackageType): Flow<List<StudyPackage>> = flowOf(emptyList())

        override fun observeInProgress(): Flow<StudyPackage?> = flowOf(null)

        override fun observeCategories(type: PackageType): Flow<List<StudyCategory>> = flowOf(emptyList())

        override fun observeSubjects(categoryId: String): Flow<List<Subject>> = flowOf(emptyList())

        override fun observeCategory(categoryId: String): Flow<StudyCategory> = unavailable()

        override fun observeTopics(subjectId: String): Flow<List<Topic>> = flowOf(emptyList())

        override fun observeSubject(subjectId: String): Flow<Subject> = unavailable()

        override fun observePackages(
            type: PackageType,
            levelLabel: String?,
        ): Flow<List<StudyPackage>> = flowOf(emptyList())

        override fun observeLevels(): Flow<List<String>> = flowOf(emptyList())

        override fun observePackageDetail(packageId: String): Flow<StudyPackageDetail> = unavailable()

        override fun observeSeasonalTryout(): Flow<SeasonalTryout?> = flowOf(null)
    }

/** The T-018 runner, result, and Pembahasan (A2c-2); Latihan and Try Out have their own API repositories. */
class HiddenExamRepository
    @Inject
    constructor() : ExamRepository {
        override suspend fun startOrResume(packageId: String): ExamSession = throw HiddenFeature()

        override suspend fun saveAnswer(
            attemptId: String,
            questionId: String,
            answer: QuestionAnswer,
        ): Unit = throw HiddenFeature()

        override suspend fun setMarked(
            attemptId: String,
            questionId: String,
            marked: Boolean,
        ): Unit = throw HiddenFeature()

        override suspend fun submit(attemptId: String): String = throw HiddenFeature()

        override fun observeResult(attemptId: String): Flow<AttemptResult> = unavailable()

        override fun observeReview(attemptId: String): Flow<List<QuestionReview>> = flowOf(emptyList())

        override fun observeHistory(): Flow<List<AttemptSummary>> = flowOf(emptyList())
    }

/** The T-018 Leaderboard; the Leaderboard tab reads the API (A2e). */
class HiddenLeaderboardRepository
    @Inject
    constructor() : LeaderboardRepository {
        override fun observeGlobal(): Flow<GlobalLeaderboard> = unavailable()

        override fun observePackageRankings(): Flow<List<PackageRanking>> = flowOf(emptyList())

        override fun observePackageLeaderboard(packageId: String): Flow<PackageLeaderboard> = unavailable()
    }

/** Langganan and paying in the app (AppFeatures.PAYMENTS). */
class HiddenSubscriptionRepository
    @Inject
    constructor() : SubscriptionRepository {
        override fun observeSubscription(): Flow<Subscription> = unavailable()

        override fun observePlans(): Flow<List<Plan>> = flowOf(emptyList())

        override fun observePaymentHistory(): Flow<List<PaymentRecord>> = flowOf(emptyList())

        override suspend fun createOrder(
            planId: String,
            method: PaymentMethodType,
            eWallet: EWallet?,
            promo: OrderPromo?,
        ): Order = throw HiddenFeature()

        override fun observeOrder(orderId: String): Flow<Order> = unavailable()

        override suspend fun confirmPayment(orderId: String): Result<Unit> = refused()
    }

/** Pengaturan and Jenjang Utama (AppFeatures.ACCOUNT_SETTINGS). */
class HiddenSettingsRepository
    @Inject
    constructor() : SettingsRepository {
        override fun observeSettings(): Flow<AccountSettings> = unavailable()

        override suspend fun setSwitch(
            switch: SettingSwitch,
            enabled: Boolean,
        ): Result<Unit> = refused()

        override suspend fun setMainLevel(level: MainLevel): Result<Unit> = refused()
    }

/** Data Pribadi and Unduh Data Akun (AppFeatures.ACCOUNT_SETTINGS, ACCOUNT_DATA_DOWNLOAD). */
class HiddenAccountDataRepository
    @Inject
    constructor() : AccountDataRepository {
        override fun observePersonalDetails(): Flow<PersonalDetails> = unavailable()

        override suspend fun requestDataCopy(categories: Set<AccountDataCategory>): Result<Unit> = refused()
    }

/** Asisten AI (AppFeatures.ASSISTANT). */
class HiddenAssistantRepository
    @Inject
    constructor() : AssistantRepository {
        override fun observeCurrentConversation(): Flow<AssistantConversation> = unavailable()

        override fun observeHistory(): Flow<List<ConversationSummary>> = flowOf(emptyList())

        override suspend fun sendMessage(
            text: String,
            voiceDurationSeconds: Int?,
            attachments: List<AssistantAttachment>,
        ): Result<Unit> = refused()

        override fun startNewConversation() = Unit

        override fun openConversation(conversationId: String) = Unit

        override suspend fun clearHistory(): Result<Unit> = refused()

        override fun listen(): Flow<String> = emptyFlow()
    }

/** The Notifikasi inbox (AppFeatures.NOTIFICATION_INBOX): nothing to read. */
class HiddenNotificationRepository
    @Inject
    constructor() : NotificationRepository {
        override fun observeNotifications(): Flow<List<AppNotification>> = flowOf(emptyList())

        override fun observeUnreadCount(): Flow<Int> = flowOf(0)

        override suspend fun markRead(notificationId: String) = Unit

        override suspend fun markAllRead() = Unit
    }

/** Paket Tersimpan (AppFeatures.SAVED_PACKAGES): nothing saved. */
class HiddenSavedPackageRepository
    @Inject
    constructor() : SavedPackageRepository {
        override fun observeSavedPackages(): Flow<List<SavedPackage>> = flowOf(emptyList())

        override fun observeIsSaved(packageId: String): Flow<Boolean> = flowOf(false)

        override suspend fun toggle(packageId: String): Boolean = throw HiddenFeature()

        override suspend fun remove(packageId: String) = Unit
    }

/** Promo (AppFeatures.PAYMENTS): no promo exists. */
class HiddenPromoRepository
    @Inject
    constructor() : PromoRepository {
        override fun observePromo(promoId: String): Flow<Promo?> = flowOf(null)
    }
