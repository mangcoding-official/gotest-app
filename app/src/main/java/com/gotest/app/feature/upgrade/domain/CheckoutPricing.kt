package com.gotest.app.feature.upgrade.domain

import com.gotest.app.core.model.Plan

/** Price breakdown shown before an order exists. */
object CheckoutPricing {
    /** [promo]'s price when it discounts [plan], else the plan price. */
    fun price(
        plan: Plan,
        promo: Promo? = null,
    ): Long = promo?.takeIf { it.planId == plan.id }?.promoPriceRupiah ?: plan.priceRupiah

    fun total(
        plan: Plan,
        promo: Promo? = null,
    ): Long = price(plan, promo) + plan.adminFeeRupiah
}
