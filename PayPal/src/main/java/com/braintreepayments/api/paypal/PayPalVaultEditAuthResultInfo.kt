package com.braintreepayments.api.paypal

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [PayPalVaultEditAuthResult.Success]
 */
data class PayPalVaultEditAuthResultInfo(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
