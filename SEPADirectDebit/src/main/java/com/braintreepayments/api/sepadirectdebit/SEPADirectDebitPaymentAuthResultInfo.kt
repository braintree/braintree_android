package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [SEPADirectDebitPaymentAuthResult.Success]
 */
data class SEPADirectDebitPaymentAuthResultInfo(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
