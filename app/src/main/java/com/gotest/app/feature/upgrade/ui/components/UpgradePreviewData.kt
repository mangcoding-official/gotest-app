package com.gotest.app.feature.upgrade.ui.components

import com.gotest.app.core.model.EWallet
import com.gotest.app.core.model.Order
import com.gotest.app.core.model.OrderStatus
import com.gotest.app.core.model.PaymentMethodType
import com.gotest.app.core.model.Plan
import com.gotest.app.core.model.PlanTier
import com.gotest.app.core.model.QrisCode
import com.gotest.app.feature.upgrade.domain.Promo

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

    val qrisOrder =
        order.copy(
            method = PaymentMethodType.Qris,
            bankName = null,
            virtualAccountNumber = null,
            qris =
                QrisCode(
                    payload =
                        "00020101021226570011ID.CO.QRIS.WWW0215ID10264123985710303UMI" +
                            "5204829953033605406110000" + "5802ID5916GoTest Indonesia6007JAKARTA6304C1F2",
                    merchantName = "GoTest Indonesia",
                    nmid = "ID1026412398571",
                ),
        )

    val eWalletOrder =
        order.copy(
            method = PaymentMethodType.EWallet,
            bankName = null,
            virtualAccountNumber = null,
            eWallet = EWallet.GoPay,
        )

    val promo =
        Promo(
            id = "promo_semangat_merdeka",
            label = "Promo 17 Agustus",
            title = "Semangat Merdeka, diskon 50% GoTest Premium",
            description =
                "Berlaku 1 – 31 Agustus 2026 untuk langganan tahunan. Kuota terbatas 1.945 pengguna pertama.",
            bannerUrl = "",
            planId = premiumPlan.id,
            productName = "GoTest Premium Tahunan",
            originalPriceRupiah = 200_000,
            promoPriceRupiah = 100_000,
            benefits =
                listOf(
                    "Akses semua tryout UTBK-SNBT dan CPNS",
                    "Pembahasan lengkap setiap soal",
                    "Buat permainan Main Bareng tanpa batas",
                    "Asisten belajar tanpa batas pesan",
                ),
            terms =
                listOf(
                    "Diskon otomatis terpasang saat checkout, tanpa kode.",
                    "Berlaku satu kali per akun, untuk paket tahunan.",
                    "Tidak dapat digabung dengan promo lain.",
                ),
        )
}
