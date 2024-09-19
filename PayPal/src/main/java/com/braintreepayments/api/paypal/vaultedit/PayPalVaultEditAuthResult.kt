package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Result of the PayPal edit vault flow received from [PayPalLauncher.handleReturnToAppFromBrowser].
 */
@ExperimentalBetaApi
sealed class PayPalVaultEditAuthResult {

    /**
     * A successful result
     */
    class Success internal constructor(
        var browserSwitchSuccess: BrowserSwitchFinalResult.Success,
    ) : PayPalVaultEditAuthResult()

    /**
     * The browser switch failed.
     */
    class Failure internal constructor(val error: Exception) : PayPalVaultEditAuthResult()

    /**
     * If no matching result
     */
    object NoResult : PayPalVaultEditAuthResult()
}
