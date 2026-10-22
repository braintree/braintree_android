package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest

/**
 * The result of starting the edit funding instrument flow.
 */
internal sealed class PayPalSavedPaymentMethodAuthRequest {

    /**
     * The request is ready to be launched with `PayPalLauncher`.
     */
    class ReadyToLaunch internal constructor(
        val paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch
    ) : PayPalSavedPaymentMethodAuthRequest()

    /**
     * There was an [error] creating the request.
     */
    class Failure internal constructor(val error: Exception) : PayPalSavedPaymentMethodAuthRequest()
}
