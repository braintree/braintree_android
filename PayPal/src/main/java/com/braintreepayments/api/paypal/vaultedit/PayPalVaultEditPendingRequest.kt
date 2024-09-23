package com.braintreepayments.api.paypal.vaultedit

/**
 * A pending request for the PayPal web-based authentication flow created by invoking
 * [PayPalLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [PayPalLauncher.handleReturnToApp]
 */
sealed class PayPalVaultEditPendingRequest {

    /**
     * A pending request was successfully started.
     *
     * @property pendingRequestString - This String should be stored and passed to
     * [PayPalLauncher.handleReturnToApp].
     */
    class Started internal constructor(val pendingRequestString: String) : PayPalVaultEditPendingRequest()

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditPendingRequest()
}
