package com.braintreepayments.api

/**
 * A request used to launch the Venmo app for continuation of the Venmo payment flow.
 */
sealed class VenmoPaymentAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [VenmoLauncher]
     */
    class ReadyToLaunch(val requestParams: VenmoPaymentAuthRequestParams) : VenmoPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : VenmoPaymentAuthRequest()
}
