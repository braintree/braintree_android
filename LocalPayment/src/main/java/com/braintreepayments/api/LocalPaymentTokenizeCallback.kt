package com.braintreepayments.api

/**
 * Callback for receiving result of
 * [LocalPaymentClient.tokenize].
 */
fun interface LocalPaymentTokenizeCallback {
    /**
     *
     * @param localPaymentResult a success, failure, or cancel result from the local payment flow
     */
    fun onLocalPaymentResult(localPaymentResult: LocalPaymentResult)
}
