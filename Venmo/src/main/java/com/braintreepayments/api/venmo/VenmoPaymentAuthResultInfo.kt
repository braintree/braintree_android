package com.braintreepayments.api.venmo

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [VenmoPaymentAuthResult.Success]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class VenmoPaymentAuthResultInfo internal constructor(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
