package com.braintreepayments.api

internal interface SEPADirectDebitInternalTokenizeCallback {
    fun onResult(sepaDirectDebitNonce: SEPADirectDebitNonce?, error: Exception?)
}
