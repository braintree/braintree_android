package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving success and error when calling
 * {@link PayPalNativeClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalNativeFlowCallback)}
 */
//TODO: i changed the name because i think one call back is enough. The clients wants to know whether it succeeded or failed
public interface PayPalNativeFlowCallback {

    /**
     * @param success if the paypal experience is successful.
     * @param error an exception that occurred while initiating PayPal Native
     */
    void onResult(PaymentMethodNonce success, Exception error);
}
