package com.braintreepayments.api

/**
 * Callback for receiving result of [PreferredPaymentMethodsClient.fetchPreferredPaymentMethods].
 * This interface is currently in beta and may be removed in future releases.
 * @hide
 */
fun interface PreferredPaymentMethodsCallback {
    /**
     * @param preferredPaymentMethodsResult [PreferredPaymentMethodsResult]
     */
    fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult)
}