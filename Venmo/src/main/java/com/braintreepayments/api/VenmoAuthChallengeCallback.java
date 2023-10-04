package com.braintreepayments.api;

public interface VenmoAuthChallengeCallback {
    void onVenmoAuthChallenge(VenmoAuthChallenge venmoAuthChallenge);

    void onVenmoError(Exception error);

}
