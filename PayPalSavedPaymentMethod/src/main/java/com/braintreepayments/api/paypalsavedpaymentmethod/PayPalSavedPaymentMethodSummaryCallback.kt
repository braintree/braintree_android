package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving the result of [PayPalSavedPaymentMethodClient.fetchFI] /
 * [PayPalSavedPaymentMethodClient.refetchFI].
 */
@ExperimentalBetaApi
fun interface PayPalSavedPaymentMethodSummaryCallback {

    /**
     * @param result a success or failure result from the saved payment method fetch
     */
    fun onPayPalSavedPaymentMethodSummaryResult(result: PayPalSavedPaymentMethodSummaryResult)
}
