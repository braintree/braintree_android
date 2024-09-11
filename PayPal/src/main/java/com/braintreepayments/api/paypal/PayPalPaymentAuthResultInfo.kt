package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [PayPalPaymentAuthResult.Success]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PayPalPaymentAuthResultInfo internal constructor(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
