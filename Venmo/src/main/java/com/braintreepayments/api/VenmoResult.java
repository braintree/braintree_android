package com.braintreepayments.api;

import androidx.annotation.Nullable;

class VenmoResult {

    private final Exception error;
    private final String paymentContextId;
    private final String venmoAccountNonce;
    private final String venmoUsername;

    VenmoResult(@Nullable String paymentContextId, @Nullable String venmoAccountNonce, @Nullable String venmoUsername, @Nullable Exception error) {
        this.paymentContextId = paymentContextId;
        this.venmoAccountNonce = venmoAccountNonce;
        this.venmoUsername = venmoUsername;
        this.error = error;
    }

    Exception getError() {
        return error;
    }

    public String getPaymentContextId() {
        return paymentContextId;
    }

    public String getVenmoAccountNonce() {
        return venmoAccountNonce;
    }

    public String getVenmoUsername() {
        return venmoUsername;
    }
}
