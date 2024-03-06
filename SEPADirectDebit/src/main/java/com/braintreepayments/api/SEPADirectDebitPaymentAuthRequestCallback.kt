package com.braintreepayments.api

/**
 * Callback for receiving result of
 * [SEPADirectDebitClient.createPaymentAuthRequest].
 */
fun interface SEPADirectDebitPaymentAuthRequestCallback {
    /**
     * @param paymentAuthRequest the result of the SEPA create mandate call. If a nonce is present,
     * no web-based mandate is required. If a nonce is not present, you
     * must trigger the web-based mandate flow via
     * [SEPADirectDebitLauncher.launch]
     */
    fun onSEPADirectDebitPaymentAuthResult(paymentAuthRequest: SEPADirectDebitPaymentAuthRequest)
}
