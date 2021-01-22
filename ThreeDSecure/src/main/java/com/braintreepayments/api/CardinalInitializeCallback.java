package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface CardinalInitializeCallback {
    void onResult(@Nullable String consumerSessionId, @Nullable Exception error);
}
