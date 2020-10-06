package com.braintreepayments.api.interfaces;

import androidx.annotation.NonNull;

import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;

/**
 * Interface for PayPal two factor authentication lookup callback
 */
public interface PayPalTwoFactorAuthCallback {

    /**
     * {@link com.braintreepayments.api.models.PayPalAccountNonce} with {@link PayPalAccountNonce#isTwoFactorAuthRequired()}
     * to determine if two factor authentication is required.
     * @param nonce returned on success
     */
    void onLookupResult(@NonNull PaymentMethodNonce nonce);

    /**
     * Called when two factor lookup was unsuccessful
     * @param exception reason for two factor lookup failure
     */
    void onLookupFailure(@NonNull Exception exception);
}

