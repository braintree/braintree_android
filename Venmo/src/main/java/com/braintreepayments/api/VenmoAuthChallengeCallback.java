package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback to handle result from
 * {@link VenmoClient#tokenizeVenmoAccount(VenmoAuthChallengeResult, VenmoResultCallback)}
 */
public interface VenmoAuthChallengeCallback {
    void onVenmoAuthChallenge(@Nullable VenmoAuthChallenge venmoAuthChallenge,
                              @Nullable Exception error);

}
