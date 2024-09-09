package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.BrowserSwitchOptions

/**
 * Returned via the [SEPADirectDebitPaymentAuthRequestCallback] after calling
 * [SEPADirectDebitClient.createPaymentAuthRequest].
 *
 * Inspect the [SEPADirectDebitNonce] property to determine if tokenization is complete, or
 * if you must continue the SEPA mandate web flow via
 * [SEPADirectDebitLauncher.launch]
 */
class SEPADirectDebitPaymentAuthRequestParams internal constructor(val browserSwitchOptions: BrowserSwitchOptions)
