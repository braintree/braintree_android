package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of the checkout result
 */
public interface PayPalNativeCheckoutResultCallback {

    /**
     * @param payPalAccountNonce {@link PayPalNativeCheckoutAccountNonce}
     * @param error              an exception that occurred while processing a PayPal result
     */
    void onResult(@Nullable PayPalNativeCheckoutAccountNonce payPalAccountNonce, @Nullable Exception error);
}
