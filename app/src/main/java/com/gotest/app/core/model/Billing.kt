package com.gotest.app.core.model

import java.time.LocalDate

data class Plan(
    val id: String,
    val tier: PlanTier,
    val name: String,
    val priceRupiah: Long,
    /** "Biaya Tambahan" added at checkout. */
    val adminFeeRupiah: Long,
    val period: String?,
    val description: String,
    val benefits: List<String>,
    val badge: String?,
)

enum class PaymentMethodType { VirtualAccount, Qris, EWallet }

/** Wallets offered under [PaymentMethodType.EWallet]. */
enum class EWallet { GoPay, Ovo, Dana, ShopeePay }

/** The code of a [PaymentMethodType.Qris] order. */
data class QrisCode(
    /** EMVCo payload the QR image is rendered from. */
    val payload: String,
    val merchantName: String,
    /** National Merchant ID printed under the code. */
    val nmid: String,
)

/** A promo price charged instead of [Plan.priceRupiah]. */
data class OrderPromo(
    val promoId: String,
    val priceRupiah: Long,
)

data class Order(
    val id: String,
    val planId: String,
    val planName: String,
    val planDetail: String,
    val method: PaymentMethodType,
    val bankName: String?,
    val virtualAccountNumber: String?,
    val priceRupiah: Long,
    val feeRupiah: Long,
    val expiresAtEpochMillis: Long,
    val status: OrderStatus,
    /** Set for [PaymentMethodType.Qris]. */
    val qris: QrisCode? = null,
    /** Set for [PaymentMethodType.EWallet]. */
    val eWallet: EWallet? = null,
    /** Promo applied at checkout; [priceRupiah] is already its price. */
    val promoId: String? = null,
) {
    val totalRupiah: Long get() = priceRupiah + feeRupiah
}

enum class OrderStatus { Pending, Paid, Expired }

data class PaymentRecord(
    val id: String,
    val planName: String,
    val paidOn: LocalDate,
    val methodLabel: String,
    val amountRupiah: Long,
    val succeeded: Boolean,
)
