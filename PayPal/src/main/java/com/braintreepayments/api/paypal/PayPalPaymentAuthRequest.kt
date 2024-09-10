package com.braintreepayments.api.paypal

/**
 * A request used to launch the continuation of the PayPal payment flow.
 */
sealed class PayPalPaymentAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [PayPalLauncher]
     */
    class ReadyToLaunch(internal val requestParams: PayPalPaymentAuthRequestParams) :
        PayPalPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : PayPalPaymentAuthRequest()
}
