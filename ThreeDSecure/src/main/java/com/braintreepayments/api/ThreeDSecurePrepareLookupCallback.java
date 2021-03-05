package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#prepareLookup(Context, ThreeDSecureRequest, ThreeDSecurePrepareLookupCallback)}.
 */
public interface ThreeDSecurePrepareLookupCallback {

    /**
     * @param request {@link ThreeDSecureRequest}
     * @param clientData JSON string of client data to be sent to server for lookup
     * @param error an exception that occurred while preparing a 3D Secure lookup
     */
    void onResult(@Nullable ThreeDSecureRequest request, @Nullable String clientData, @Nullable Exception error);
}
