package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
sealed class PayPalVaultEditResult {
    class Success internal constructor(val riskCorrelationId: String) : PayPalVaultEditResult()
    class Failure internal constructor(val error: Exception) : PayPalVaultEditResult()
    object Cancel : PayPalVaultEditResult()
}
