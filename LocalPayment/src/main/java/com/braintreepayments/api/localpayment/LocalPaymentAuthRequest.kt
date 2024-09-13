package com.braintreepayments.api.localpayment

/**
 * A request used to launch the continuation of the local payment flow.
 */
sealed class LocalPaymentAuthRequest {

    // TODO: make requestParams internal when LocalPaymentClientUnitTest is converted to Kotlin
    /**
     * The request was successfully created and is ready to be launched by [LocalPaymentLauncher]
     * @param requestParams this parameter is intended for internal use only. It is not covered by
     * semantic versioning and may be changed or removed at any time.
     */
    class ReadyToLaunch(val requestParams: LocalPaymentAuthRequestParams) :
        LocalPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : LocalPaymentAuthRequest()
}
