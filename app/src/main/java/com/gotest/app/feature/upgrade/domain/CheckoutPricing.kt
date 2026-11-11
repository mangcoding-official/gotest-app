package com.gotest.app.feature.upgrade.domain

import com.gotest.app.core.model.Plan

/** Price breakdown shown before an order exists. */
object CheckoutPricing {
    fun total(plan: Plan): Long = plan.priceRupiah + plan.adminFeeRupiah
}
