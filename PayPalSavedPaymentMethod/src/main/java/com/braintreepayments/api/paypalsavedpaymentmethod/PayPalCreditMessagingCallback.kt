package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Callback for receiving the result of
 * [PayPalSavedPaymentMethodClient.fetchCreditPresentmentMessages].
 */
@ExperimentalBetaApi
fun interface PayPalCreditMessagingCallback {

    /**
     * @param result the fetched [PayPalCreditMessagingContent], or null if the fetch fails or
     * returns no `preferred_message` - callers should hide the messaging row; the FI card still
     * renders.
     */
    fun onPayPalCreditMessagingResult(result: PayPalCreditMessagingContent?)
}
