package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link PayPalNativeCheckoutClient#onBrowserSwitchResult(BrowserSwitchResult, PayPalNativeCheckoutBrowserSwitchResultCallback)}.
 */
public interface PayPalNativeCheckoutBrowserSwitchResultCallback {

    /**
     * @param payPalAccountNonce {@link PayPalNativeCheckoutAccountNonce}
     * @param error              an exception that occurred while processing a PayPal result
     */
    void onResult(@Nullable PayPalNativeCheckoutAccountNonce payPalAccountNonce, @Nullable Exception error);
}
