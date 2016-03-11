package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.models.UnionPayCapabilities;
import com.braintreepayments.api.models.UnionPayCardBuilder;

/**
 * Interface that defines callbacks for UnionPay.
 */
public interface UnionPayListener extends BraintreeListener {

    /**
     * Will be called when
     * {@link com.braintreepayments.api.models.UnionPayCapabilities} has been successfully fetched.
     */
    void onCapabilitiesFetched(UnionPayCapabilities capabilities);

    /**
     * Will be called when the customer has been enrolled. EnrollmentId, and the SMS code collected from
     * the customer will applied to the {@link UnionPayCardBuilder} before invoking
     * {@link com.braintreepayments.api.UnionPay#tokenize(BraintreeFragment, UnionPayCardBuilder)}
     * @param enrollmentId returned from
     * {@link com.braintreepayments.api.UnionPay#enroll(BraintreeFragment, UnionPayCardBuilder)}.
     */
    void onSmsCodeSent(String enrollmentId);
}
