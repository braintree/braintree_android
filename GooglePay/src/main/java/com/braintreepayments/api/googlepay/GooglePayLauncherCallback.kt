package com.braintreepayments.api.googlepay

/**
 * Callback used to instantiate a [GooglePayLauncher] to handle Activity results from the Google Pay
 * payment flow
 */
fun interface GooglePayLauncherCallback {

    fun onGooglePayLauncherResult(googlePayPaymentAuthResult: GooglePayPaymentAuthResult?)
}
