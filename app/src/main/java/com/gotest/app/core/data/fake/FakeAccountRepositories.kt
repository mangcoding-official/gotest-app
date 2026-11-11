package com.gotest.app.core.data.fake

import com.gotest.app.core.domain.repository.AuthRepository
import com.gotest.app.core.domain.repository.SubscriptionRepository
import com.gotest.app.core.domain.repository.UserRepository
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.OrderStatus
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TRIAL_DAYS = 30L
private const val MIN_PASSWORD_LENGTH = 8
private const val PREMIUM_PRICE = 100_000L
private const val ADMIN_FEE = 10_000L
private const val PAYMENT_WINDOW_HOURS = 6L
private const val PAYMENT_WINDOW_MINUTES = 25L

/** In-memory account shared by the auth, profile and subscription fakes. */
@Singleton
class FakeAccountStore
    @Inject
    constructor() {
        val profile: MutableStateFlow<UserProfile> =
            MutableStateFlow(
                UserProfile(
                    id = "user_gilang",
                    fullName = "Gilang Permana",
                    email = "gilangpermana@mail.com",
                    avatarUrl = drawableUri("avatar_gilang"),
                    stats =
                        UserStats(
                            questionsAnswered = 100,
                            accuracyPercent = 86,
                            packagesCompleted = 412,
                            questionsTrendPercent = -24,
                            accuracyTrendPercent = 24,
                            packagesTrendPercent = 24,
                        ),
                    subscription =
                        Subscription(
                            tier = PlanTier.FreeTrial,
                            planName = "Paket Free Trial",
                            activatedOn = LocalDate.now(),
                            activeUntil = LocalDate.now().plusDays(TRIAL_DAYS),
                            paymentMethod = null,
                        ),
                ),
            )
        val signedIn = MutableStateFlow(false)
    }

@Singleton
class FakeAuthRepository
    @Inject
    constructor(
        private val store: FakeAccountStore,
    ) : AuthRepository {
        override val isSignedIn: StateFlow<Boolean> = store.signedIn.asStateFlow()

        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<Unit> {
            simulateLatency()
            // Any well-formed email with an 8+ character password signs in.
            if (!email.contains('@') || password.length < MIN_PASSWORD_LENGTH) {
                return Result.failure(IllegalArgumentException("invalid credentials"))
            }
            store.signedIn.value = true
            return Result.success(Unit)
        }

        override suspend fun signInWithGoogle(): Result<Unit> {
            simulateLatency()
            store.signedIn.value = true
            return Result.success(Unit)
        }

        override suspend fun register(
            fullName: String,
            email: String,
            password: String,
        ): Result<Unit> {
            simulateLatency()
            store.profile.update { it.copy(fullName = fullName, email = email) }
            store.signedIn.value = true
            return Result.success(Unit)
        }

        override suspend fun requestPasswordReset(email: String): Result<Unit> {
            simulateLatency()
            return Result.success(Unit)
        }

        override suspend fun signOut() {
            simulateLatency()
            store.signedIn.value = false
        }
    }

@Singleton
class FakeUserRepository
    @Inject
    constructor(
        private val store: FakeAccountStore,
    ) : UserRepository {
        override fun observeProfile(): Flow<UserProfile> = store.profile.withFakeLatency()

        override suspend fun updateProfile(
            fullName: String,
            email: String,
        ): Result<Unit> {
            simulateLatency()
            store.profile.update { it.copy(fullName = fullName, email = email) }
            return Result.success(Unit)
        }

        override suspend fun updateAvatar(avatarUrl: String): Result<Unit> {
            simulateLatency()
            store.profile.update { it.copy(avatarUrl = avatarUrl) }
            return Result.success(Unit)
        }

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> {
            simulateLatency()
            return if (newPassword.length >= MIN_PASSWORD_LENGTH && newPassword != currentPassword) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("weak password"))
            }
        }

        override suspend fun deleteAccount(): Result<Unit> {
            simulateLatency()
            store.signedIn.value = false
            return Result.success(Unit)
        }
    }

@Singleton
class FakeSubscriptionRepository
    @Inject
    constructor(
        private val store: FakeAccountStore,
    ) : SubscriptionRepository {
        private val plans =
            listOf(
                Plan(
                    id = "plan_premium_yearly",
                    tier = PlanTier.Premium,
                    name = "Premium Tahunan",
                    priceRupiah = PREMIUM_PRICE,
                    adminFeeRupiah = ADMIN_FEE,
                    period = "tahun",
                    description = "Satu tahun persiapan belajar dengan lebih tenang. Rata-rata hanya Rp8.333/bulan.",
                    benefits =
                        listOf(
                            "Akses semua paket latihan & tryout",
                            "Pembahasan lengkap setiap soal",
                            "Leaderboard & analisis per topik",
                            "Akses selama 12 bulan",
                        ),
                    badge = "Paling Hemat",
                ),
                Plan(
                    id = "plan_free_trial",
                    tier = PlanTier.FreeTrial,
                    name = "Free Trial",
                    priceRupiah = 0,
                    adminFeeRupiah = 0,
                    period = null,
                    description = "Coba GoTest dan mulai perjalanan belajarmu.",
                    benefits =
                        listOf(
                            "Akses latihan selama masa trial",
                            "Paket khusus akun trial",
                            "Melihat hasil pengerjaan",
                        ),
                    badge = null,
                ),
            )

        private val history =
            MutableStateFlow(
                listOf(
                    PaymentRecord(
                        id = "pay_2025",
                        planName = "Premium Tahunan",
                        paidOn = LocalDate.of(2025, 10, 21),
                        methodLabel = "VA BCA",
                        amountRupiah = PREMIUM_PRICE + ADMIN_FEE,
                        succeeded = true,
                    ),
                ),
            )

        private val orders = MutableStateFlow<Map<String, Order>>(emptyMap())

        override fun observeSubscription(): Flow<Subscription> = store.profile.map { it.subscription }.withFakeLatency()

        override fun observePlans(): Flow<List<Plan>> = flowOf(plans).withFakeLatency()

        override fun observePaymentHistory(): Flow<List<PaymentRecord>> = history.withFakeLatency()

        override suspend fun createOrder(
            planId: String,
            method: PaymentMethodType,
        ): Order {
            simulateLatency()
            val plan = plans.first { it.id == planId }
            val window =
                TimeUnit.HOURS.toMillis(PAYMENT_WINDOW_HOURS) - TimeUnit.MINUTES.toMillis(PAYMENT_WINDOW_MINUTES)
            val order =
                Order(
                    id = "order_${UUID.randomUUID()}",
                    planId = plan.id,
                    planName = "GoTest Premium",
                    planDetail = "Langganan Tahunan • 12 Bulan",
                    method = method,
                    bankName = if (method == PaymentMethodType.VirtualAccount) "BCA" else null,
                    virtualAccountNumber =
                        if (method ==
                            PaymentMethodType.VirtualAccount
                        ) {
                            "1241 2319 8513 12"
                        } else {
                            null
                        },
                    priceRupiah = plan.priceRupiah,
                    feeRupiah = plan.adminFeeRupiah,
                    expiresAtEpochMillis = System.currentTimeMillis() + window,
                    status = OrderStatus.Pending,
                )
            orders.update { it + (order.id to order) }
            return order
        }

        override fun observeOrder(orderId: String): Flow<Order> =
            orders.map { it[orderId] }.filterNotNull().withFakeLatency()

        override suspend fun confirmPayment(orderId: String): Result<Unit> {
            simulateLatency()
            val order = orders.value[orderId] ?: return Result.failure(IllegalStateException("unknown order"))
            orders.update { it + (orderId to order.copy(status = OrderStatus.Paid)) }
            val today = LocalDate.now()
            store.profile.update {
                it.copy(
                    subscription =
                        Subscription(
                            tier = PlanTier.Premium,
                            planName = "GoTest Premium Tahunan",
                            activatedOn = today,
                            activeUntil = today.plusYears(1),
                            paymentMethod = "Virtual Account BCA",
                        ),
                )
            }
            history.update {
                listOf(
                    PaymentRecord(
                        id = order.id,
                        planName = "Premium Tahunan",
                        paidOn = today,
                        methodLabel = "VA BCA",
                        amountRupiah = order.totalRupiah,
                        succeeded = true,
                    ),
                ) + it
            }
            return Result.success(Unit)
        }
    }
