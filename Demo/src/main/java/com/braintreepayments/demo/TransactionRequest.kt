package com.braintreepayments.demo

data class TransactionRequest(
    val amount: String,
    val paymentMethodNonce: String,
    val merchantAccountId: String?,
    val threeDSecureRequired: Boolean
)
