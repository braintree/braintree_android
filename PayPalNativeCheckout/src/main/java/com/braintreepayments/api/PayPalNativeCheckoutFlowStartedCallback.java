package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link PayPalNativeCheckoutClient#requestOneTimePayment(FragmentActivity, PayPalNativeCheckoutRequest, PayPalNativeCheckoutFlowStartedCallback)} and
 * {@link PayPalNativeCheckoutClient#requestBillingAgreement(FragmentActivity, PayPalNativeCheckoutVaultRequest, PayPalNativeCheckoutFlowStartedCallback)} and
 * {@link PayPalNativeCheckoutClient#tokenizePayPalAccount(FragmentActivity, PayPalNativeRequest, PayPalNativeCheckoutFlowStartedCallback)}.
 */
public interface PayPalNativeCheckoutFlowStartedCallback {

    /**
     * @param error an exception that occurred while initiating a PayPal transaction
     */
    void onResult(@Nullable Exception error);
}
