package com.braintreepayments.api.paypal

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [PayPalPaymentAuthResult.Success]
 */
data class PayPalPaymentAuthResultInfo internal constructor(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
