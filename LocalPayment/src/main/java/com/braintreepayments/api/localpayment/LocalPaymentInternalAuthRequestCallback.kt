package com.braintreepayments.api.localpayment

/**
 * Callback for receiving result of [LocalPaymentClient.createPaymentAuthRequest].
 */
internal fun interface LocalPaymentInternalAuthRequestCallback {

    /**
     * @param localPaymentAuthRequestParams [LocalPaymentAuthRequestParams]
     * @param error an exception that occurred while initiating a Local Payment
     */
    fun onLocalPaymentInternalAuthResult(
        localPaymentAuthRequestParams: LocalPaymentAuthRequestParams?,
        error: Exception?
    )
}
