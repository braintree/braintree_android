package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link PayPalClient#onBrowserSwitchResult(BrowserSwitchResult, PayPalBrowserSwitchResultCallback)}.
 */
public interface PayPalBrowserSwitchResultCallback {

    /**
     * @param payPalAccountNonce {@link PayPalAccountNonce}
     * @param error              an exception that occurred while processing a PayPal result
     */
    void onResult(@NonNull PayPalResult result);
}
