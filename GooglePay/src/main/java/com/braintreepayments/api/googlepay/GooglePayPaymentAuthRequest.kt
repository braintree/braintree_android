package com.braintreepayments.api.googlepay

/**
 * A request used to launch the continuation of the Google Pay flow.
 */
sealed class GooglePayPaymentAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [GooglePayLauncher]
     */
    class ReadyToLaunch(internal val requestParams: GooglePayPaymentAuthRequestParams) : GooglePayPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : GooglePayPaymentAuthRequest()
}
