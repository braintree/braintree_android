package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link UnionPayClient#fetchCapabilities(String, UnionPayFetchCapabilitiesCallback)}.
 */
public interface UnionPayFetchCapabilitiesCallback {

    /**
     * @param capabilities {@link UnionPayCapabilities}
     * @param error an exception that occurred while fetching Union Pay capabilities
     */
    void onResult(@Nullable UnionPayCapabilities capabilities, @Nullable Exception error);
}
