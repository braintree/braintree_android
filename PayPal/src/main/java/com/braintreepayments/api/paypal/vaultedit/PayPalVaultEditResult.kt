package com.braintreepayments.api.paypal.vaultedit

sealed class PayPalVaultEditResult {
    class Success(val riskCorrelationId: String) : PayPalVaultEditResult()
    class Failure(val error: Exception) : PayPalVaultEditResult()
    object Cancel : PayPalVaultEditResult()
}