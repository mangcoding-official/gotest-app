package com.gotest.app.feature.upgrade.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.gotest.app.feature.upgrade.ui.checkout.CheckoutRoute
import com.gotest.app.feature.upgrade.ui.payment.PaymentPendingRoute
import com.gotest.app.feature.upgrade.ui.plan.UpgradePlanRoute
import com.gotest.app.feature.upgrade.ui.success.PaymentSuccessRoute
import com.gotest.app.navigation.CheckoutDestination
import com.gotest.app.navigation.EditProfileDestination
import com.gotest.app.navigation.PaymentPendingDestination
import com.gotest.app.navigation.PaymentSuccessDestination
import com.gotest.app.navigation.SubscriptionDestination
import com.gotest.app.navigation.TopLevelDestination
import com.gotest.app.navigation.UpgradePlanDestination
import com.gotest.app.navigation.navigateBackToHome

/** Upgrade Akun → Checkout → Menunggu Pembayaran → Pembayaran Berhasil. */
fun NavGraphBuilder.upgradeScreens(navController: NavController) {
    composable<UpgradePlanDestination> {
        UpgradePlanRoute(
            onClose = navController::navigateUp,
            onChoosePlan = { planId -> navController.navigate(CheckoutDestination(planId)) },
        )
    }
    composable<CheckoutDestination> {
        CheckoutRoute(
            onBack = navController::navigateUp,
            onEditProfile = { navController.navigate(EditProfileDestination) },
            onOpenPayment = { orderId ->
                // Back from the payment screen returns to the plans, not to a stale checkout.
                navController.navigate(PaymentPendingDestination(orderId)) {
                    popUpTo<CheckoutDestination> { inclusive = true }
                }
            },
        )
    }
    composable<PaymentPendingDestination> {
        PaymentPendingRoute(
            onBack = navController::navigateUp,
            onPaymentSuccess = navController::navigateToPaymentSuccess,
        )
    }
    composable<PaymentSuccessDestination> {
        PaymentSuccessRoute(
            onOpenDashboard = {
                navController.navigateBackToHome()
            },
            onOpenTransaction = {
                navController.navigate(SubscriptionDestination) {
                    popUpTo<PaymentSuccessDestination> { inclusive = true }
                }
            },
        )
    }
}

/** Replaces the whole upgrade flow with the success screen, so Back leaves the flow. */
private fun NavController.navigateToPaymentSuccess(orderId: String) {
    val flowStartsAtPlans = runCatching { getBackStackEntry<UpgradePlanDestination>() }.isSuccess
    navigate(PaymentSuccessDestination(orderId)) {
        if (flowStartsAtPlans) {
            popUpTo<UpgradePlanDestination> { inclusive = true }
        } else {
            popUpTo<PaymentPendingDestination> { inclusive = true }
        }
    }
}
