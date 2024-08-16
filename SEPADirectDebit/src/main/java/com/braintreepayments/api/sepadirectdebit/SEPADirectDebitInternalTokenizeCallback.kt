package com.braintreepayments.api.sepadirectdebit

fun interface SEPADirectDebitInternalTokenizeCallback {
    fun onResult(sepaDirectDebitNonce: SEPADirectDebitNonce?, error: Exception?)
}
