package com.braintreepayments.api.localpayment

/**
 * A pending request for the local payment web-based authentication flow created by invoking
 * [LocalPaymentLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [LocalPaymentLauncher.handleReturnToApp]
 */
sealed class LocalPaymentPendingRequest {

    /**
     * A pending request was successfully started.
     *
     * @property pendingRequestString - This String should be stored and passed to
     * [LocalPaymentLauncher.handleReturnToApp].
     */
    class Started(val pendingRequestString: String) : LocalPaymentPendingRequest()

    /**
     * An error occurred launching the local payment browser flow. See [error] for details.
     */
    class Failure internal constructor(val error: Exception) : LocalPaymentPendingRequest()
}
