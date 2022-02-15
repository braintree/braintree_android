package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface VenmoApiCallback {

    void onResult(@Nullable String paymentContextId, @Nullable Exception exception);
}
