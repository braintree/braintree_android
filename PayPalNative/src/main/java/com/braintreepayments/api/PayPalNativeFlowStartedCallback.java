package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving error when calling
 * {@link PayPalNativeClient#tokenizePayPalAccount(FragmentActivity, PayPalNativeFlowStartedCallback)}
 */
public interface PayPalNativeFlowStartedCallback {

    /**
     * @param error an exception that occurred while initiating PayPal Native
     */
    void onResult(Exception error);
}
