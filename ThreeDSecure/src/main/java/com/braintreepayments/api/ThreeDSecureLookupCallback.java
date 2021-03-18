package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#performLookup(FragmentActivity, ThreeDSecureRequest, ThreeDSecureLookupCallback)}.
 */
public interface ThreeDSecureLookupCallback {

    /**
     * @param request {@link ThreeDSecureRequest}
     * @param lookup {@link ThreeDSecureLookup}
     * @param error an exception that occurred while performing a 3D Secure verification
     */
    void onResult(ThreeDSecureRequest request, ThreeDSecureLookup lookup, Exception error);
}
