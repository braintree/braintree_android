package com.braintreepayments.api.uicomponents

/**
 * A pending request for the PayPal web-based authentication flow created when [PayPalButton]
 * is clicked. This pending request should be stored locally within the app or on-device and used
 * to complete the payment flow in [PayPalButton.handleReturnToApp]
 */
sealed class PayPalButtonPendingRequest {

    /**
     * The PayPal flow was successfully launched.
     *
     * @property pendingRequest - This String should be stored and passed to
     * [PayPalButton.handleReturnToApp].
     */
    class Launched(val pendingRequest: String) : PayPalButtonPendingRequest()

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : PayPalButtonPendingRequest()
}