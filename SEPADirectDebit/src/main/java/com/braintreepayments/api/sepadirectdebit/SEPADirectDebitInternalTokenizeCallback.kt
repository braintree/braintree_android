package com.braintreepayments.api.sepadirectdebit

internal fun interface SEPADirectDebitInternalTokenizeCallback {
    fun onResult(sepaDirectDebitNonce: SEPADirectDebitNonce?, error: Exception?)
}
