package com.braintreepayments.api;

public interface PayPalRequestCallback {
    // TODO: Change to single parameter exception callback when API is finalized
    void onResult(boolean requestInitiated, Exception error);
}
