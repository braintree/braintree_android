package com.braintreepayments.api.googlepay

import com.google.android.gms.wallet.PaymentDataRequest

/**
 * Used to request Google Pay payment authorization via
 * [GooglePayLauncher.launch]
 */
class GooglePayPaymentAuthRequestParams internal constructor(
    val googlePayEnvironment: Int,
    val paymentDataRequest: PaymentDataRequest
)
