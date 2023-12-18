package com.braintreepayments.api;

import android.content.Context;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#prepareLookup(Context, ThreeDSecureRequest,
 * ThreeDSecurePrepareLookupCallback)}.
 */
public interface ThreeDSecurePrepareLookupCallback {

    /**
     * @param prepareLookupResult {@link ThreeDSecurePrepareLookupResult}
     */
    void onPrepareLookupResult(ThreeDSecurePrepareLookupResult prepareLookupResult);
}