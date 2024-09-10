package com.braintreepayments.api.googlepay

import com.google.android.gms.wallet.PaymentData

/**
 * Result returned from the callback used to instantiate [GooglePayLauncher] that should be
 * passed to [GooglePayClient.tokenize]
 */
class GooglePayPaymentAuthResult internal constructor(
    internal val paymentData: PaymentData?,
    internal val error: Exception?
)
