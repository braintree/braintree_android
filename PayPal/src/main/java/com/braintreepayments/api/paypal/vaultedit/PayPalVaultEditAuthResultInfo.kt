package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.ExperimentalBetaApi

/**
 * Details of a [PayPalVaultEditAuthResult.Success]
 */
@ExperimentalBetaApi
data class PayPalVaultEditAuthResultInfo(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
