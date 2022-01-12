package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface VenmoListener {
    void onVenmoTokenizeSuccess(@NonNull VenmoAccountNonce venmoAccountNonce);
    void onVenmoTokenizeError(@NonNull Exception error);
}
