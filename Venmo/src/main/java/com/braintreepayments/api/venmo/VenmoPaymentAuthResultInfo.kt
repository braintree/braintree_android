package com.braintreepayments.api.venmo

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [VenmoPaymentAuthResult.Success]
 */
data class VenmoPaymentAuthResultInfo internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
