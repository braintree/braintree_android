package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Interface for ThreeDSecure prepareLookup callbacks.
 */
public interface ThreeDSecurePrepareLookupCallback {
    /**
     * Called when the 3DS preparation is completed.
     *
     * @param request the {@link ThreeDSecureRequest} with a nonce.
     * @param clientData JSON string of client data to be sent to server for lookup.
     */
    void onResult(@Nullable ThreeDSecureRequest request, @Nullable String clientData, @Nullable Exception error);
}
