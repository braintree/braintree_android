package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {
    class Success(val riskCorrelationId: String) : PayPalVaultEditResult()
    class Failure(val error: Exception) : PayPalVaultEditResult()
    object Cancel : PayPalVaultEditResult()
}
