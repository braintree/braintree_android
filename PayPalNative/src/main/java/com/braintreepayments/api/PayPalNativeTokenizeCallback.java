package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving success and error when calling
 * {@link PayPalNativeClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalNativeTokenizeCallback)}
 */
public interface PayPalNativeTokenizeCallback {

    /**
     * @param payPalAccountNonce {@link PayPalAccountNonce}
     *  @param error an exception that occurred during the PayPal Native flow
     */
    void onResult(PayPalAccountNonce payPalAccountNonce, Exception error);
}
