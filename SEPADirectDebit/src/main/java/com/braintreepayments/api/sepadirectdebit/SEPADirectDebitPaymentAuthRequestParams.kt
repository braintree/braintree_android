package com.braintreepayments.api.sepadirectdebit

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Returned via the [SEPADirectDebitPaymentAuthRequestCallback] after calling
 * [SEPADirectDebitClient.createPaymentAuthRequest].
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SEPADirectDebitPaymentAuthRequestParams internal constructor(val browserSwitchOptions: BrowserSwitchOptions)
