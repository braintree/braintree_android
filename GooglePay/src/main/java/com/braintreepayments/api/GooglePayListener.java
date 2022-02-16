package com.braintreepayments.api;


import androidx.annotation.NonNull;

/**
 * Implement this interface to receive results from the Google Pay flow.
 */
public interface GooglePayListener {

    /**
     * Called when a {@link GooglePayCardNonce} is created without error.
     * @param googlePayCardNonce a {@link GooglePayCardNonce}
     */
    void onGooglePaySuccess(@NonNull GooglePayCardNonce googlePayCardNonce);

    /**
     * Called when there was an error during the Google Pay flow.
     * @param error explains reason for Google Pay failure.
     */
    void onGooglePayFailure(@NonNull Exception error);
}
