package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface KountDataCollectorCallback {
    void onResult(@Nullable String kountSessionId, @Nullable Exception error);
}
