package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link PayPalClient#onBrowserSwitchResult(BrowserSwitchResult, PayPalTokenizeCallback)}.
 */
public interface PayPalTokenizeCallback {

    /**
     * @param payPalAccountNonce {@link PayPalAccountNonce}
     * @param error              an exception that occurred while processing a PayPal result
     */
    void onResult(@Nullable PayPalAccountNonce payPalAccountNonce, @Nullable Exception error);
}
