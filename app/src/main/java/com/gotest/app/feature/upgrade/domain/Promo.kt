package com.gotest.app.feature.upgrade.domain

import kotlinx.coroutines.flow.Flow

private const val PERCENT = 100

/** A price promo on one plan ("Beranda / Promo Semangat Merdeka"). */
data class Promo(
    val id: String,
    /** "Promo 17 Agustus" chip; also marks the promo at checkout. */
    val label: String,
    val title: String,
    /** Period and quota line under the title. */
    val description: String,
    /** Coil model of the banner artwork. */
    val bannerUrl: String,
    /** Plan the promo discounts; "Ambil Promo" opens its checkout. */
    val planId: String,
    /** "GoTest Premium Tahunan". */
    val productName: String,
    /** Struck-through price. */
    val originalPriceRupiah: Long,
    /** Price charged at checkout, before the admin fee. */
    val promoPriceRupiah: Long,
    /** "Yang kamu dapat". */
    val benefits: List<String>,
    /** "Syarat & Ketentuan". */
    val terms: List<String>,
) {
    /** 50 for "-50%". */
    val discountPercent: Int
        get() = ((originalPriceRupiah - promoPriceRupiah) * PERCENT / originalPriceRupiah).toInt()
}

interface PromoRepository {
    /** Emits null when the promo does not exist or has ended. */
    fun observePromo(promoId: String): Flow<Promo?>
}
