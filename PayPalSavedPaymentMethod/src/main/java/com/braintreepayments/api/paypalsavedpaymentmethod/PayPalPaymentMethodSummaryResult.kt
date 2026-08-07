package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Result of [SavedPayPalPaymentMethodClient.fetchFI] / [SavedPayPalPaymentMethodClient.refetchFI].
 */
@ExperimentalBetaApi
sealed class PayPalPaymentMethodSummaryResult {

    /**
     * The fetch succeeded. A No-FI read is still a [Success] with empty
     * [PayPalPaymentMethodSummary.paymentMethods] and a null [PayPalPaymentMethodSummary.payer].
     */
    class Success internal constructor(
        val paymentMethodSummary: PayPalPaymentMethodSummary
    ) : PayPalPaymentMethodSummaryResult()

    /**
     * The fetch failed. [error] is a [PayPalPaymentMethodSummaryException] for a missing JWT or a
     * server `errors[]` response (carrying `errorClass`), or the underlying network exception.
     */
    class Failure internal constructor(val error: Exception) : PayPalPaymentMethodSummaryResult()
}
