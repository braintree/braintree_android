package com.braintreepayments.api

/**
 * A pending request for the PayPal web-based authentication flow created by invoking
 * [PayPalLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [PayPalLauncher.handleReturnToAppFromBrowser]
 */
sealed class PayPalPendingRequest {

    /**
     * A pending request was successfully started. This [PayPalPendingRequest.Started] should be
     * stored and passed to [PayPalLauncher.handleReturnToAppFromBrowser]
     */
    class Started(val request: PayPalBrowserSwitchRequest) : PayPalPendingRequest()

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : PayPalPendingRequest()
}