package com.braintreepayments.api.localpayment

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [LocalPaymentAuthResult.Success]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LocalPaymentAuthResultInfo internal constructor(
    internal val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
