package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to receive PayPal result notifications.
 */
public interface PayPalNativeCheckoutListener {

    /**
     * Called when PayPal tokenization is complete without error.
     * @param payPalAccountNonce PayPal tokenization result
     */
    void onPayPalSuccess(@NonNull PayPalNativeCheckoutAccountNonce payPalAccountNonce);

    /**
     * Called when PayPal tokenization has failed with an error.
     * @param error explains reason for PayPal failure.
     */
    void onPayPalFailure(@NonNull Exception error);
}