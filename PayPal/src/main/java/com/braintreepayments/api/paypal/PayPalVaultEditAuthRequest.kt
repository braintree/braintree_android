package com.braintreepayments.api.paypal

/**
 * A request used to launch the continuation of the PayPal Edit Vault flow.
 */
sealed class PayPalVaultEditAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [PayPalLauncher]
     */
    class ReadyToLaunch internal constructor(
        internal val requestParams: PayPalVaultEditAuthRequestParams
    ) : PayPalVaultEditAuthRequest()

    /**
     * There was an [error] creating the request.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditAuthRequest()
}
