package com.braintreepayments.api.sepadirectdebit

/**
 * Callback for receiving result of [SEPADirectDebitClient.tokenize]
 */
fun interface SEPADirectDebitTokenizeCallback {
    /**
     * @param sepaDirectDebitResult a success, failure, or cancel result from the SEPA Direct Debit
     * flow
     */
    fun onSEPADirectDebitResult(sepaDirectDebitResult: SEPADirectDebitResult)
}
