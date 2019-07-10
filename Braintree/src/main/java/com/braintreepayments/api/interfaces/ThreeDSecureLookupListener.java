package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecureRequest;

/**
 * Interface for ThreeDSecure callbacks.
 * Interface that can be used to review the {@link ThreeDSecureLookup} before continuing tokenization.
 */
public interface ThreeDSecureLookupListener {
    /**
     * Called when the 3DS Lookup result is ready.
     *
     * 3DS can be continued by invoking:
     * {@link com.braintreepayments.api.ThreeDSecure#continuePerformVerification(BraintreeFragment, ThreeDSecureRequest, ThreeDSecureLookup)}
     *
     * @param request The request used to generate the lookup.
     * @param lookup Lookup details that can be reviewed.
     */
    void onLookupComplete(ThreeDSecureRequest request,
                          ThreeDSecureLookup lookup);
}
