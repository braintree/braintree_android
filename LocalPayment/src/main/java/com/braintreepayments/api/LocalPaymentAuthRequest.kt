package com.braintreepayments.api

/**
 * A request used to launch the continuation of the local payment flow.
 */
sealed class LocalPaymentAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [LocalPaymentLauncher]
     */
    class ReadyToLaunch(val requestParams: LocalPaymentAuthRequestParams) :
        LocalPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : LocalPaymentAuthRequest()
}
