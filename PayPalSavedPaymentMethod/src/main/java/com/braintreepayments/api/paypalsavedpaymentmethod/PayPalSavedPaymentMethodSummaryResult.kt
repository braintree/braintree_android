package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Result of [PayPalSavedPaymentMethodClient.fetchFI] / [PayPalSavedPaymentMethodClient.refetchFI].
 */
@ExperimentalBetaApi
sealed class PayPalSavedPaymentMethodSummaryResult {

    /**
     * The fetch succeeded. A No-FI read is still a [Success] with empty
     * [PayPalSavedPaymentMethodSummary.paypalSavedPaymentMethods] and a null
     * [PayPalSavedPaymentMethodSummary.paypalPayer].
     */
    class Success internal constructor(
        val paymentMethodSummary: PayPalSavedPaymentMethodSummary
    ) : PayPalSavedPaymentMethodSummaryResult()

    /**
     * The fetch failed. [error] is a [PayPalSavedPaymentMethodSummaryException] for a missing JWT or a
     * server `errors[]` response (carrying `errorClass`), or the underlying network exception.
     */
    class Failure internal constructor(val error: Exception) : PayPalSavedPaymentMethodSummaryResult()
}
