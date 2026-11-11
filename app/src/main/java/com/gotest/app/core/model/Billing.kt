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
