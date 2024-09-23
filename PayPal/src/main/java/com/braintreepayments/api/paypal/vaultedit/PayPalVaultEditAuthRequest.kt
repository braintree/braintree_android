package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * A request used to launch the continuation of the PayPal Edit Vault flow.
 */
@ExperimentalBetaApi
sealed class PayPalVaultEditAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [PayPalLauncher]
     *
     * @property riskCorrelationId This ID is used to link subsequent retry attempts if payment is declined
     */
    class ReadyToLaunch internal constructor(
        val riskCorrelationId: String,
        internal var browserSwitchOptions: BrowserSwitchOptions?,
    ) : PayPalVaultEditAuthRequest()

    /**
     * There was an [error] creating the request.
     */
    class Failure internal constructor(
        val error: Exception
    ) : PayPalVaultEditAuthRequest()
}
