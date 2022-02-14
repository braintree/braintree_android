package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to receive Local Payment result notifications.
 */
public interface LocalPaymentListener {

    /**
     * Called when Local Payment tokenization is complete without error.
     * @param localPaymentNonce Local Payment tokenization result
     */
    void onLocalPaymentTokenizeSuccess(@NonNull LocalPaymentNonce localPaymentNonce);

    /**
     * Called when Local Payment tokenization has failed with an error.
     * @param error explains reason for PayPal failure.
     */
    void onLocalPaymentFailure(@NonNull Exception error);
}
