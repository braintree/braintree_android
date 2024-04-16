package com.braintreepayments.api.sepadirectdebit

internal interface SEPADirectDebitInternalTokenizeCallback {
    fun onResult(sepaDirectDebitNonce: SEPADirectDebitNonce?, error: Exception?)
}
