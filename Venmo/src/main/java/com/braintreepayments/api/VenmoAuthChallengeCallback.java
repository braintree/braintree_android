package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback to handle result from
 * {@link VenmoClient#tokenize(VenmoPaymentAuthResult, VenmoResultCallback)}
 */
public interface VenmoAuthChallengeCallback {
    void onVenmoAuthChallenge(@Nullable VenmoPaymentAuthRequest venmoPaymentAuthRequest,
                              @Nullable Exception error);

}
