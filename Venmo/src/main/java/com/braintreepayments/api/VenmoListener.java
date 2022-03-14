package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Implement this interface to receive results from the Venmo payment flow.
 */
public interface VenmoListener {

    /**
     * Called when a {@link VenmoAccountNonce} is created without error.
     * @param venmoAccountNonce a {@link VenmoAccountNonce}
     */
    void onVenmoSuccess(@NonNull VenmoAccountNonce venmoAccountNonce);

    /**
     * Called when there was an error during the Venmo payment flow.
     * @param error explains reason for Venmo failure.
     */
    void onVenmoFailure(@NonNull Exception error);
}
