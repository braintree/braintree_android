package com.braintreepayments.api.paypal

/**
 * A request used to launch the continuation of the PayPal payment flow.
 */
sealed class PayPalPaymentAuthRequest {

    // TODO: make requestParams internal when PayPalClientUnitTest is converted to Kotlin
    /**
     * The request was successfully created and is ready to be launched by [PayPalLauncher]
     * @param requestParams this parameter is intended for internal use only. It is not covered by
     * semantic versioning and may be changed or removed at any time.
     */
    class ReadyToLaunch internal constructor(val requestParams: PayPalPaymentAuthRequestParams) :
        PayPalPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure internal constructor(val error: Exception) : PayPalPaymentAuthRequest()
}
