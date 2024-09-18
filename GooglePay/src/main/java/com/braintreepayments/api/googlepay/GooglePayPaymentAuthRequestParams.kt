package com.braintreepayments.api.googlepay

import androidx.annotation.RestrictTo
import com.google.android.gms.wallet.PaymentDataRequest

/**
 * Used to request Google Pay payment authorization via
 * [GooglePayLauncher.launch]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayPaymentAuthRequestParams internal constructor(
    val googlePayEnvironment: Int,
    val paymentDataRequest: PaymentDataRequest
)
