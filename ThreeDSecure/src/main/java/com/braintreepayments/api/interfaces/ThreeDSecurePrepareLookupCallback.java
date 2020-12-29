package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.ThreeDSecureRequest;

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
    void onResult(ThreeDSecureRequest request, String clientData, Exception error);
}
