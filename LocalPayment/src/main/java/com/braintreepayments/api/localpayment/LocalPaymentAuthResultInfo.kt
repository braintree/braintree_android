package com.braintreepayments.api.localpayment

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [LocalPaymentAuthResult.Success]
 */
class LocalPaymentAuthResultInfo internal constructor
    (private val browserSwitchSuccess: BrowserSwitchFinalResult.Success) {

    val browserSwitchResultInfo: BrowserSwitchFinalResult.Success
        get() = browserSwitchSuccess
}
