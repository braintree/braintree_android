package com.braintreepayments.api.sepadirectdebit

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchFinalResult

/**
 * Details of a [SEPADirectDebitPaymentAuthResult.Success]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SEPADirectDebitPaymentAuthResultInfo(
    val browserSwitchSuccess: BrowserSwitchFinalResult.Success
)
