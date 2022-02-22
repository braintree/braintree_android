package com.braintreepayments.api;


import androidx.annotation.NonNull;

/**
 * Implement this interface to receive results from the Google Pay flow.
 */
public interface GooglePayListener {

    /**
     * Called when a {@link PaymentMethodNonce} is created without error.
     * @param paymentMethodNonce a {@link PaymentMethodNonce}
     */
    void onGooglePaySuccess(@NonNull PaymentMethodNonce paymentMethodNonce);

    /**
     * Called when there was an error during the Google Pay flow.
     * @param error explains reason for Google Pay failure.
     */
    void onGooglePayFailure(@NonNull Exception error);
}
