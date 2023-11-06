package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link PayPalClient#createPaymentAuthRequest(FragmentActivity, PayPalRequest, PayPalFlowStartedCallback)}.
 */
public interface PayPalFlowStartedCallback {

    /**
     * @param payPalResponse the result of the PayPal web authentication flow
     * @param error          an exception that occurred while initiating a PayPal transaction
     */
    void onResult(PayPalResponse payPalResponse, @Nullable Exception error);
}
