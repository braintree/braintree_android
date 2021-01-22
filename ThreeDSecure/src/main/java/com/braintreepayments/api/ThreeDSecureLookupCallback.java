package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Interface for ThreeDSecure callbacks.
 * Interface that can be used to review the {@link ThreeDSecureLookup} before continuing tokenization.
 */
public interface ThreeDSecureLookupCallback {
    /**
     * Called when the 3DS Lookup result is ready.
     *
     * 3DS can be continued by invoking:
     * {@link ThreeDSecureClient#continuePerformVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureLookup, ThreeDSecureResultCallback)}
     *
     * @param error Exception (if applicable)
     * @param request the {@link ThreeDSecureRequest} used to generate the lookup.
     * @param lookup Lookup details that can be reviewed.
     */
    void onResult(ThreeDSecureRequest request, ThreeDSecureLookup lookup, Exception error);
}
