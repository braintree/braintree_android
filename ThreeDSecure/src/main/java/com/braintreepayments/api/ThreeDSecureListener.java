package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to receive 3DS verification notifications.
 */
public interface ThreeDSecureListener {
    /**
     * Called when 3DS verification is complete without error.
     * @param threeDSecureResult 3DS verification result
     */
    void onThreeDSecureSuccess(@NonNull ThreeDSecureResult threeDSecureResult);

    /**
     * Called when 3DS verification has failed with an error.
     * @param error explains reason for 3DS failure.
     */
    void onThreeDSecureFailure(@NonNull Exception error);
}
