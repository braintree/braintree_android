package com.braintreepayments.api.googlepay

import androidx.annotation.RestrictTo
import com.google.android.gms.wallet.PaymentData

/**
 * Result returned from the callback used to instantiate [GooglePayLauncher] that should be
 * passed to [GooglePayClient.tokenize]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayPaymentAuthResult internal constructor(
    val paymentData: PaymentData?,
    val error: Exception?
)
