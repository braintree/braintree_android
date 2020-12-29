package com.braintreepayments.api;

public interface VenmoAuthorizeAccountCallback {

    // TODO: Change to single parameter exception callback when API is finalized
    void onResult(boolean isAuthorized, Exception error);
}
