package com.braintreepayments.api;

import com.braintreepayments.api.models.UnionPayCapabilities;

public interface UnionPayFetchCapabilitiesCallback {
    void onResult(UnionPayCapabilities capabilities, Exception error);
}
