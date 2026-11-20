package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalPendingRequest

/** Receives the pending request when [PayPalSavedPaymentMethodView] opens the PayPal paysheet for an edit. */
@ExperimentalBetaApi
fun interface PayPalSavedPaymentMethodLaunchCallback {

    /**
     * Called once the PayPal paysheet is open. Store [pendingRequest] (for example in shared preferences, so it
     * survives process death) and pass it to [PayPalSavedPaymentMethodView.handleReturnToApp] when the buyer returns
     * to your app. Clear it once that call returns `true`; a result lost to process death after that isn't delivered.
     */
    fun onPayPalPendingRequest(pendingRequest: PayPalPendingRequest.Started)
}
