package com.gotest.app.feature.upgrade.ui.components

import com.gotest.app.core.model.Order
import com.gotest.app.core.model.OrderStatus
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.PlanTier

/** Sample data for the upgrade previews, copied from the design. */
internal object UpgradePreviewData {
    val premiumPlan =
        Plan(
            id = "plan_premium_yearly",
            tier = PlanTier.Premium,
            name = "Premium Tahunan",
            priceRupiah = 100_000,
            adminFeeRupiah = 10_000,
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
        )

    val freePlan =
        Plan(
            id = "plan_free_trial",
            tier = PlanTier.FreeTrial,
            name = "Free Trial",
            priceRupiah = 0,
            adminFeeRupiah = 0,
            period = null,
            description = "Coba GoTest dan mulai perjalanan belajarmu.",
            benefits = listOf("Akses latihan selama masa trial", "Paket khusus akun trial", "Melihat hasil pengerjaan"),
            badge = null,
        )

    val order =
        Order(
            id = "order_preview",
            planId = premiumPlan.id,
            planName = "GoTest Premium",
            planDetail = "Langganan Tahunan • 12 Bulan",
            method = PaymentMethodType.VirtualAccount,
            bankName = "BCA",
            virtualAccountNumber = "1241 2319 8513 12",
            priceRupiah = 100_000,
            feeRupiah = 10_000,
            expiresAtEpochMillis = 0,
            status = OrderStatus.Pending,
        )
}
