package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.UnionPayCapabilities;

public interface UnionPayFetchCapabilitiesCallback {
    void onResult(@Nullable UnionPayCapabilities capabilities, @Nullable Exception error);
}
