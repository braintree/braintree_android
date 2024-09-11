package com.braintreepayments.api.googlepay

/**
 * A request used to launch the continuation of the Google Pay flow.
 */
sealed class GooglePayPaymentAuthRequest {

    // TODO: make requestParams internal when GooglePayClientUnitTest is converted to Kotlin
    /**
     * The request was successfully created and is ready to be launched by [GooglePayLauncher]
     * @param requestParams this parameter is intended for internal use only. It is not covered by
     * semantic versioning and may be changed or removed at any time.
     */
    class ReadyToLaunch(val requestParams: GooglePayPaymentAuthRequestParams) : GooglePayPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : GooglePayPaymentAuthRequest()
}
