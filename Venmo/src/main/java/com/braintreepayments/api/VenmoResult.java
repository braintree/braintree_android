package com.braintreepayments.api;

import androidx.annotation.Nullable;

class VenmoResult {

    private final VenmoAccountNonce venmoAccountNonce;
    private final Exception error;

    VenmoResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error) {
        this.venmoAccountNonce = venmoAccountNonce;
        this.error = error;
    }

    VenmoAccountNonce getVenmoAccountNonce() {
        return venmoAccountNonce;
    }

    Exception getError() {
        return error;
    }
}
