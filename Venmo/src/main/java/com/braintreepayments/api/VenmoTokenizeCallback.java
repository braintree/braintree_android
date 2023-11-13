package com.braintreepayments.api;

import androidx.annotation.NonNull;

public interface VenmoTokenizeCallback {

    void onVenmoResult(@NonNull VenmoPaymentResult result);
}
