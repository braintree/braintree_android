package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#onBrowserSwitchResult(BrowserSwitchResult)}.
 */
public interface LocalPaymentBrowserSwitchResultCallback {

    /**
     * @param localPaymentNonce {@link LocalPaymentNonce}
     * @param error an exception that occurred while processing Local Payment result
     */
    void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error);
}
