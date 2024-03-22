package com.braintreepayments.api.googlepay

/**
 * Callback for receiving result of [GooglePayClient.isReadyToPay] and
 * [GooglePayClient.isReadyToPay].
 */
fun interface GooglePayIsReadyToPayCallback {

    /**
     * @param googlePayReadinessResult a [GooglePayReadinessResult] signifying if a user is ready to
     * pay.
     */
    fun onGooglePayReadinessResult(googlePayReadinessResult: GooglePayReadinessResult?)
}
