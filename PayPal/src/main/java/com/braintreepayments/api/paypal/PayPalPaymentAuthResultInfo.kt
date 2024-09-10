package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [PayPalPaymentAuthResult.Success]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PayPalPaymentAuthResultInfo internal constructor(
    internal val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
