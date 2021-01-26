package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface UnionPayFetchCapabilitiesCallback {
    void onResult(@Nullable UnionPayCapabilities capabilities, @Nullable Exception error);
}
