package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link PayPalNativeCheckoutClient#requestOneTimePayment(FragmentActivity, PayPalCheckoutRequest, PayPalFlowStartedCallback)} and
 * {@link PayPalNativeCheckoutClient#requestBillingAgreement(FragmentActivity, PayPalVaultRequest, PayPalFlowStartedCallback)} and
 * {@link PayPalNativeCheckoutClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest, PayPalFlowStartedCallback)}.
 */
public interface PayPalFlowStartedCallback {

    /**
     * @param error an exception that occurred while initiating a PayPal transaction
     */
    void onResult(@Nullable Exception error);
}
