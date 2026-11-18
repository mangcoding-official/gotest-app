package com.gotest.app.feature.account.ui.components

import com.gotest.app.core.model.PaymentRecord
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.model.Subscription
import com.gotest.app.core.model.UserProfile
import com.gotest.app.core.model.UserStats
import com.gotest.app.feature.account.domain.AccountSettings
import com.gotest.app.feature.account.domain.MainLevel
import com.gotest.app.feature.account.domain.PersonalDetails
import com.gotest.app.feature.account.domain.SettingSwitch
import java.time.LocalDate
import java.time.LocalTime

/** Sample data for the account @Preview functions only (texts and dates from the design frames). */
internal object AccountPreviewData {
    val trialSubscription =
        Subscription(
            tier = PlanTier.FreeTrial,
            planName = "Paket Free Trial",
            activatedOn = LocalDate.of(2026, 9, 23),
            activeUntil = LocalDate.of(2026, 10, 23),
            paymentMethod = null,
        )

    val premiumSubscription =
        Subscription(
            tier = PlanTier.Premium,
            planName = "GoTest Premium Tahunan",
            activatedOn = LocalDate.of(2026, 10, 21),
            activeUntil = LocalDate.of(2027, 10, 28),
            paymentMethod = "Virtual Account BCA",
        )

    val profile =
        UserProfile(
            id = "user_gilang",
            fullName = "Gilang Permana",
            email = "gilangpermana@mail.com",
            avatarUrl = null,
            stats =
                UserStats(
                    questionsAnswered = 100,
                    accuracyPercent = 86,
                    packagesCompleted = 412,
                    questionsTrendPercent = 0,
                    accuracyTrendPercent = 0,
                    packagesTrendPercent = 0,
                ),
            subscription = trialSubscription,
        )

    val payments =
        listOf(
            PaymentRecord(
                id = "pay_2026",
                planName = "Premium Tahunan",
                paidOn = LocalDate.of(2026, 10, 21),
                methodLabel = "VA BCA",
                amountRupiah = 110_000,
                succeeded = true,
            ),
            PaymentRecord(
                id = "pay_2025",
                planName = "Premium Tahunan",
                paidOn = LocalDate.of(2025, 10, 21),
                methodLabel = "VA BCA",
                amountRupiah = 110_000,
                succeeded = true,
            ),
        )

    val settings =
        AccountSettings(
            switches =
                mapOf(
                    SettingSwitch.ActivityResults to true,
                    SettingSwitch.RankingUpdates to true,
                    SettingSwitch.NewPackages to false,
                    SettingSwitch.SubscriptionStatus to true,
                    SettingSwitch.StudyReminder to true,
                    SettingSwitch.PromoEmails to false,
                ),
            reminderTime = LocalTime.of(19, 0),
            mainLevel = MainLevel.UtbkSnbt,
        )

    val personalDetails =
        PersonalDetails(
            phoneNumber = "0812-3456-7890",
            birthDate = LocalDate.of(2008, 5, 14),
            school = "SMA Negeri 3 Bandung",
            city = "Bandung, Jawa Barat",
        )
}
