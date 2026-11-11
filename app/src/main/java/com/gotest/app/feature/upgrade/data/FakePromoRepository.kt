package com.gotest.app.feature.upgrade.data

import com.gotest.app.core.data.fake.drawableUri
import com.gotest.app.core.data.fake.withFakeLatency
import com.gotest.app.feature.upgrade.domain.Promo
import com.gotest.app.feature.upgrade.domain.PromoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/** Id of the promo behind Beranda's "Semangat Merdeka" banner (see `HOME_PROMO_ID`). */
private const val SEMANGAT_MERDEKA_PROMO_ID = "promo_semangat_merdeka"

/** The one promo of the design, copied from "Beranda / Promo Semangat Merdeka". */
@Singleton
class FakePromoRepository
    @Inject
    constructor() : PromoRepository {
        private val promos =
            listOf(
                Promo(
                    id = SEMANGAT_MERDEKA_PROMO_ID,
                    label = "Promo 17 Agustus",
                    title = "Semangat Merdeka, diskon 50% GoTest Premium",
                    description =
                        "Berlaku 1 – 31 Agustus 2026 untuk langganan tahunan. " +
                            "Kuota terbatas 1.945 pengguna pertama.",
                    bannerUrl = drawableUri("banner_promo"),
                    planId = "plan_premium_yearly",
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
                ),
            )

        override fun observePromo(promoId: String): Flow<Promo?> =
            flowOf(promos.firstOrNull { it.id == promoId }).withFakeLatency()
    }
