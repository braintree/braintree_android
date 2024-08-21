package com.braintreepayments.api.localpayment


/**
 * Details of a [LocalPaymentAuthResult.Success]
 */
class LocalPaymentAuthResultInfo internal constructor(browserSwitchSuccess: Success) {
    private val browserSwitchSuccess: Success = browserSwitchSuccess

    val browserSwitchResultInfo: Success
        get() = browserSwitchSuccess
}
