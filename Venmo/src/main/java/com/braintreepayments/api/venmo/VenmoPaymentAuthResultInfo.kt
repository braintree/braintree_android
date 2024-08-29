package com.braintreepayments.api.venmo

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [VenmoPaymentAuthResult.Success]
 */
class VenmoPaymentAuthResultInfo internal constructor
    (private val browserSwitchSuccess: BrowserSwitchFinalResult.Success) {

    val browserSwitchResultInfo: BrowserSwitchFinalResult.Success
        get() = browserSwitchSuccess
}
