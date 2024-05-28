package com.braintreepayments.api.paypal

/**
 * A request used to launch the Venmo app for continuation of the PayPal payment flow.
 */
sealed class PayPalPaymentAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [PayPalLauncher]
     */
    class ReadyToLaunch(val requestParams: PayPalPaymentAuthRequestParams) :
        PayPalPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : PayPalPaymentAuthRequest()
}
